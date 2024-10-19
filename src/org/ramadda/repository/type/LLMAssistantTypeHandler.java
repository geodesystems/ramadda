/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;

import org.json.*;
import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;


import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
help:
https://platform.openai.com/docs/assistants/quickstart?context=without-streaming
 */
public class LLMAssistantTypeHandler extends GenericTypeHandler {

    private static final String ACTION_ASSISTANT = "llmassistant";
    private static final String ACTION_UPLOAD = "llmassistant_upload";
    private static final String ARG_THREAD = "thread";


    private static final String URL_ASSISTANTS= "https://api.openai.com/v1//assistants";
    private static final String URL_VECTOR_STORES= "https://api.openai.com/v1/vector_stores";
    private static final String URL_VECTOR_STORES_FILES= "https://api.openai.com/v1/vector_stores/${vector_store_id}/files";
    private static final String URL_FILES="https://api.openai.com/v1/files";

    private static final String URL_THREADS = "https://api.openai.com/v1/threads";
    private static final String URL_MESSAGES= "https://api.openai.com/v1/threads/${thread}/messages";
    private static final String URL_RUNS= "https://api.openai.com/v1/threads/${thread}/runs";



    public LLMAssistantTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    public boolean canBeCreatedBy(Request request) {
        return request.getUser().getAdmin();
    }

    private Result handleError(Request request, String msg) {
	String s =  JsonUtil.map(Utils.makeListFromValues("error", JU.quote(msg)));
	return  new Result("", new StringBuilder(s), JsonUtil.MIMETYPE);
    }

    private IO.Result call(Request request, URL url, String body) throws Exception {
	return getRepository().getLLMManager().call(getRepository().getLLMManager().getOpenAIJobManager(),
						    url, body,
						    "OpenAI-Beta","assistants=v2",
						    "Content-Type","application/json",
						    "Authorization","Bearer " +getKey());
    
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
	String assistantId = entry.getStringValue(request,"assistant_id","");
	if(Utils.stringDefined(assistantId)) {
	    return;
	}
	List<String> args = new ArrayList<String>();
	/*curl "https://api.openai.com/v1/assistants" 
	  "instructions": "You are a personal math tutor. Write and run code to answer math questions.",
	  "name": "Math Tutor",
	  "tools": [{"type": "file_search"}],
	  "model": "gpt-4o"
	*/
	Utils.add(args,"tools","[{\"type\": \"file_search\"}]",
		  "model",JU.quote("gpt-4o"),
		  "name",JU.quote(entry.getName()),
		  "instructions",JU.quote(entry.getStringValue(request,"instructions","")));
	IO.Result result=call(request,   new URL(URL_ASSISTANTS), JU.map(args));

	if(result.getError()) {
	    throw new IllegalStateException("LLMAssistant: Error creating assistant:" + result.getResult());
	}
	assistantId =  getId(result.getResult());
	entry.setValue("assistant_id",assistantId);

	//create a vector store
	args = new ArrayList<String>();
	Utils.add(args,  "name",JU.quote(entry.getName() +" Vector Store"));
	result=call(request,   new URL(URL_VECTOR_STORES), JU.map(args));
	if(result.getError()) {
	    throw new IllegalStateException("LLMAssistant: Error creating vector store:" + result.getResult());
	}

	String vectorStoreId = getId(result.getResult());
	entry.setValue("vector_store_id",vectorStoreId);

	//associate the vector store with the assistant
	/*  curl https://api.openai.com/v1/assistants/asst_abc123 
	    -d '{"tool_resources": {"file_search": {"vector_store_ids": []}},}'*/

	args = new ArrayList<String>();
	Utils.add(args,  "tool_resources",JU.map("file_search",JU.map("vector_store_ids",JU.list(JU.quote(vectorStoreId)))));
	result=call(request,   new URL(URL_ASSISTANTS+"/" + assistantId), JU.map(args));
	if(result.getError()) {
	    throw new IllegalStateException("LLMAssistant: Error adding vector store:" + result.getResult());
	}
    }


    private IO.Result  uploadFile(Request request, Entry entry, File file) throws Exception {
	List postArgs = new ArrayList();
	Utils.add(postArgs,"purpose","assistants","file",file);
	IO.Result result =IO.doMultipartPost(new URL(URL_FILES),
					     new String[]{"Authorization"," Bearer " +getKey()},
					     postArgs);
	return result;
    }

    private String getId(String json) throws Exception {
	return new JSONObject(json).getString("id");
    }

    /**
       curl https://api.openai.com/v1/assistants/asst_abc123	\
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2" \
  -d '{
      "instructions": "You are an HR bot, and you have access to files to answer employee questions about company policies. Always response with info from either of the files.",
      "tools": [{"type": "file_search"}],
      "model": "gpt-4o"
    }'
    */


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
	if(!tag.equals(ACTION_ASSISTANT))
	    return super.getWikiInclude(wikiUtil, request, originalEntry, entry, tag, props);

	if(!getLLMManager().isOpenAIEnabled()) {
	    return HU.span("LLM Assistant: OpenAI access is not enabled",HU.cssClass("ramadda-wiki-error"));
	}


	String assistantId = entry.getStringValue(request,"assistant_id","");
	if(!Utils.stringDefined(assistantId)) {
	    return HU.span("LLM Assistant: No assistant ID is specified",HU.cssClass("ramadda-wiki-error"));
	}


	StringBuilder sb = new StringBuilder();
	if(request.isAdmin()) {
	    String url = "https://platform.openai.com/playground/assistants?assistant=" +assistantId;
	    sb.append(HU.href(url,"@OpenAI",HU.attrs("target","openai")));
	    sb.append("<br>");
	}
	String thread= getThread(request, entry);
	if(thread==null) {
	    return HU.span("LLM Assistant: Could not create thread",HU.cssClass("ramadda-wiki-error"));
	}
        String id = HU.getUniqueId("chat_div");
	HU.div(sb,"",HU.attrs("style","width:100%;","id", id));
	HU.importJS(sb,getPageHandler().makeHtdocsUrl("/wiki.js"));
	HU.importJS(sb,getPageHandler().makeHtdocsUrl("/documentchat.js"));
	List<String> args = new ArrayList<String>();
	Utils.add(args,"thread",JU.quote(thread));
	HU.script(sb, HU.call("new DocumentChat", HU.squote(id),HU.squote(entry.getId()),
			      JU.quote(ACTION_ASSISTANT),"null",JU.map(args)));
        return sb.toString();
    }


    private String getThread(Request request, Entry entry) throws Exception {
	String thread  = request.getString(ARG_THREAD,null);
	if(thread!=null) {
	    //	    System.err.println("from arg:" + thread);
	    return thread;
	}
	String sessionKey = "llmassistantthread_" + entry.getId();
	if(thread==null) {
	    thread =(String)getSessionManager().getSessionProperty(request,sessionKey);
	    //	    System.err.println("from session:" + thread);
	}
	if(thread==null) {
	    /*curl https://api.openai.com/v1/threads	\
	      -H "Content-Type: application/json"	\
	      -H "Authorization: Bearer $OPENAI_API_KEY"	\
	      -H "OpenAI-Beta: assistants=v2"			\
	      -d ''*/

	    IO.Result result=call(request,  new URL(URL_THREADS), "");
	    thread = result.getResult();
	    if(thread!=null) {
		JSONObject obj     = new JSONObject(thread);
		thread = obj.optString("id",null);
		//		System.err.println("THREAD:" + thread);
		if(thread!=null) {
		    getSessionManager().putSessionProperty(request,sessionKey,thread);
		}
	    }
	}

	return thread;
    }

    private String getKey() {
	return getLLMManager().getOpenAIKey();
    }

    public Result processEntryAction(Request request, Entry entry)
	throws Exception {
	try {
	    return processEntryActionInner(request, entry);

	} catch(Exception exc) {
	    getLogManager().logError("LLMAssistant: " + entry.getName() +" error:" + exc,exc);
	    return handleError(request,"Error processing request:" + exc);
	}
    }

    private Result processUpload(Request request, Entry entry)
	throws Exception {
	if(!request.isAdmin()) {
	    throw new IllegalStateException("You must be an admin user to upload a file");
	}
	StringBuilder sb = new StringBuilder();
	getPageHandler().entrySectionOpen(request, entry, sb,"LLM Assistant File Upload");
	sb.append(request.uploadForm(getRepository().URL_ENTRY_ACTION,""));
	sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
	sb.append(HU.hidden(ARG_ACTION, ACTION_UPLOAD));
	sb.append("Upload a file to OpenAI:");
	sb.append("<br>");
	sb.append(HU.fileInput(ARG_FILE, ""));
	sb.append("<p>");
	sb.append(HU.submit("Upload"));
	sb.append(HU.formClose());

	if(request.exists(ARG_FILE)) {
	    try {
		File file = new File(request.getUploadedFile(ARG_FILE));
		IO.Result  fileResult = uploadFile(request, entry,  file);
		file.delete();
		if(fileResult.getError()) {
		    sb.append(getPageHandler().showDialogError("Error uploading file:" + fileResult.getResult()));
		} else {
		    String fileId = getId(fileResult.getResult());
		    URL url = new URL(URL_VECTOR_STORES_FILES.replace("${vector_store_id}",entry.getStringValue(request,"vector_store_id","")));
		    IO.Result result= call(request, url, JU.map("file_id",JU.quote(fileId)));
		    if(result.getError()) {
			sb.append(getPageHandler().showDialogError(result.getResult()));
		    } else {
			sb.append(getPageHandler().showDialogNote("File has been uploaded. It may take a bit before it is available"));
		    }
		}
	    } catch(Exception exc) {
		sb.append(getPageHandler().showDialogError(exc.toString()));
	    }
	}

	getPageHandler().entrySectionClose(request, entry, sb);
	return new Result("LLM Upload",sb);
    }

    private Result processEntryActionInner(Request request, Entry entry)
	throws Exception {
        String action = request.getString("action", "");
	if(action.equals(ACTION_UPLOAD)) {
	    return processUpload(request, entry);
	}

	if (!action.equals(ACTION_ASSISTANT)) {
	    return super.processEntryAction(request,entry);
	}
	String assistantId = entry.getStringValue(request,"assistant_id","");
	if(!Utils.stringDefined(assistantId)) {
	    return handleError(request,"no assistant ID defined");
	}


	String thread= getThread(request, entry);
	if(thread==null) {
	    return handleError(request,"Unable to create thread");
	}


	/*
curl https://api.openai.com/v1/threads/thread_abc123/messages 
      "role": "user",
      "content": "I need to solve the equation `3x + 11 = 14`. Can you help me?"
	 */

	String  messagesUrl = URL_MESSAGES.replace("${thread}",thread);
	//	messagesUrl ="https://api.openai.com/v1/threads/thread_AoWuuLPxAexvGH4VfVICkiCR/messages";
	List<String> message = new ArrayList<String>();
	String q = request.getString("question","");
	Utils.add(message,"role",JU.quote("user"), "content", JU.quote(q));
	IO.Result result=call(request,   new URL(messagesUrl), JU.map(message));
	if(result.getError()) {
	    System.err.println("ERROR:" + result.getResult());
	    return handleError(request, result.getResult());
	}

	/* curl https://api.openai.com/v1/threads/thread_abc123/runs 
	   "assistant_id": "asst_abc123",
	   "instructions": "Please address the user as Jane Doe. The user has a premium account."
	*/

	List<String> run = new ArrayList<String>();
	Utils.add(run,"assistant_id",JU.quote(assistantId), "instructions", JU.quote("Please process the question"));
	String  runsUrl = URL_RUNS.replace("${thread}",thread);
	IO.Result runResult= call(request, new URL(runsUrl), JU.map(run));
	if(runResult.getError()) {
	    return handleError(request, runResult.getResult());
	}
	String runId = new JSONObject(runResult.getResult()).optString("id","");
	int cnt = 20;
	int sleep = 500;
	//Give OpenAI a bit to process
	Misc.sleep(500);
	while(cnt-->0) {
	    IO.Result finalResult=call(request,   new URL(messagesUrl+"?limit=10&run_id=" + runId), null);
	    if(finalResult.getError()) {
		return handleError(request, finalResult.getResult());
	    }
	    JSONObject obj = new JSONObject(finalResult.getResult());
	    JSONArray data=obj.optJSONArray("data");
	    for (int i = 0; i < data.length(); i++) {
		JSONObject dataObject =data.getJSONObject(i);
		if(dataObject.optString("role","").equals("assistant")) {
		    JSONArray content=dataObject.getJSONArray("content");
		    for (int j = 0; j < content.length(); j++) {
			JSONObject contentObject =content.getJSONObject(j);
			JSONObject text = contentObject.optJSONObject("text",null);
			if(text!=null) {
			    String value =  text.getString("value");
			    value = value.replace("\n\n","<p>");
			    value = value.replaceAll("【.*?】","");
			    //			    System.err.println("VALUE:" + value);
			    String s =  JsonUtil.map(Utils.makeListFromValues("response", JU.quote(value)));
			    return  new Result("", new StringBuilder(s), JsonUtil.MIMETYPE);
			}
		    }
		}
	    }
	    //	    System.err.println("No results: sleeping:" + sleep +" result:" + finalResult.getResult().replace("\n"," "));
	    Misc.sleep(sleep);
	    sleep= Math.min(5000,(int)(sleep*1.5));

	}


	StringBuilder sb = new  StringBuilder();
	String s =  JsonUtil.mapAndQuote(Utils.makeListFromValues("error", "The call to OpenAI timed out"));
	return  new Result("", new StringBuilder(s), JsonUtil.MIMETYPE);
    }

}
