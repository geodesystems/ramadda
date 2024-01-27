/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.repository;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.admin.*;
import org.ramadda.repository.metadata.MetadataManager;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.net.URL;

import org.json.*;

import java.util.concurrent.*;

/**
 * Handles LLM requests
 *
 */
@SuppressWarnings("unchecked")
public class LLMManager extends  AdminHandlerImpl {

    public static boolean  debug = true;

    public static final String PROP_OPENAI_KEY = "openai.api.key";
    public static final String PROP_GEMINI_KEY = "gemini.api.key";	

    public static final int TOKEN_LIMIT_GEMINI = 4000;
    public static final int TOKEN_LIMIT_GPT3 = 2000;    
    public static final int TOKEN_LIMIT_GPT4 = 4000;

    private static final Object MUTEX_GEMINI = new Object();
    private static final Object MUTEX_OPENAI = new Object();    

    public static final String MODEL_WHISPER_1 = "whisper-1";
    public static final String MODEL_GPT_3_5="gpt-3.5-turbo-1106";
    public static final String MODEL_GPT_4="gpt-4";
    public static final String MODEL_GPT_VISION  = "gpt-4-vision-preview";
    public static final String MODEL_GEMINI = "gemini";

    public static final String ARG_USEGPT4  = "usegpt4";
    public static final String ARG_MODEL = "model";

    public static final String ARG_EXTRACT_KEYWORDS = "extract_keywords";
    public static final String ARG_EXTRACT_SUMMARY = "extract_summary";    

    public static final String ARG_EXTRACT_AUTHORS = "extract_authors";
    public static final String ARG_EXTRACT_TITLE = "extract_title";	        



    public static final String URL_OPENAI_TRANSCRIPTION = "https://api.openai.com/v1/audio/transcriptions";
    public static final String URL_OPENAI_COMPLETION =  "https://api.openai.com/v1/chat/completions";
    public static final String URL_GEMINI="https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    public static final String PROMPT_TITLE="Extract the title from the following document. Your result should be in proper case form. The document text is:";
    public static final String PROMPT_KEYWORDS= "Extract keywords from the following text. The return format should be comma delimited keywords. Limit your response to no more than 10 keywords:";
    public static final String PROMPT_SUMMARY = "Summarize the following text. \nAssume the reader has a college education. \nLimit the summary to no more than 4 sentences.";

    private JobManager openAIJobManager;
    private JobManager geminiJobManager;    

    private Object NEXT_MUTEX= new Object();

    public LLMManager(Repository repository) {
        super(repository);
	openAIJobManager = new JobManager(repository,
					  repository.getProperty("ramadda.llm.openai.threads",2));
	//	openAIJobManager.setDebug(true);
	geminiJobManager = new JobManager(repository,
					  repository.getProperty("ramadda.llm.gemini.threads",2));
	//	geminiJobManager.setDebug(true);	
    }

    public void debug(String msg) {
	if(debug) System.err.println("LLMManager:" + msg);
    }

    public String getId() {
        return "llmmanager";
    }


    private List<String> openAIKeys;
    private int openAIKeyIdx=0;
    private synchronized String getOpenAIKey() {
	if(openAIKeys==null) {
	    String openAIKey = getRepository().getProperty(PROP_OPENAI_KEY);
	    if(openAIKey!=null) openAIKeys = Utils.split(openAIKey,",",true,true);
	    else openAIKeys = new ArrayList<String>();
	}
	if(openAIKeys.size()==0) {
	    return null;
	}	    
	openAIKeyIdx++;
	if(openAIKeyIdx>=openAIKeys.size()) openAIKeyIdx=0;
	return openAIKeys.get(openAIKeyIdx);
    }


    private boolean isGeminiEnabled() {
	return  Utils.stringDefined(getRepository().getProperty(PROP_GEMINI_KEY));
    }


    private boolean isOpenAIEnabled() {
	return Utils.stringDefined(getRepository().getProperty(PROP_OPENAI_KEY));
    }


    public boolean isGPT4Enabled() {
	return getRepository().getProperty("openai.gpt4.enabled",false);
    }    

    
    public boolean isLLMEnabled() {
	return isGeminiEnabled() || isOpenAIEnabled();
    }


    public String getNewEntryExtract(Request request) {
	if(!isLLMEnabled()) return "";
	String space = HU.space(3);
	StringBuilder sb = new StringBuilder();
	List<HtmlUtils.Selector> models = new ArrayList<HtmlUtils.Selector>();
	if(isOpenAIEnabled()) {
	    models.add(new HtmlUtils.Selector("OpenAI-GPT3.5",MODEL_GPT_3_5));
	    if(isGPT4Enabled()) {
		models.add(new HtmlUtils.Selector("OpenAI-GPT4.0",MODEL_GPT_4));
	    }
	}
	if(isGeminiEnabled()) {
	    models.add(new HtmlUtils.Selector("Google Gemini",MODEL_GEMINI));
	}
	if(models.size()==0) return "";
	if(models.size()==1) {
	    sb.append(HU.hidden(ARG_MODEL,models.get(0).getId()));
	} else {
	    sb.append(msgLabel("Model"));
	    sb.append(HU.space(1));
	    sb.append(HU.select(ARG_MODEL,models,""));
	    sb.append("<br>");
	}

	HU.labeledCheckbox(sb,ARG_EXTRACT_TITLE, "true", request.get(ARG_EXTRACT_TITLE,false),  "","Extract title");
	sb.append(space);
	HU.labeledCheckbox(sb, ARG_EXTRACT_SUMMARY, "true", request.get(ARG_EXTRACT_SUMMARY,false), "","Extract summary");
	sb.append(space);
	HU.labeledCheckbox(sb, ARG_EXTRACT_KEYWORDS, "true", request.get(ARG_EXTRACT_KEYWORDS, false),  "","Extract keywords");
	sb.append(space);
	HU.labeledCheckbox(sb, ARG_EXTRACT_AUTHORS, "true", request.get(ARG_EXTRACT_AUTHORS,false),"","Extract authors");
	sb.append("<br>");

	sb.append("Note: when extracting keywords, title, etc., the file text is sent to the <a href=https://openai.com/api/>OpenAI GPT API</a> for processing.<br>There will also be a delay before the results are shown for the new entry.");
	return sb.toString();
    }



    public Result processLLM(Request request)  throws Exception {
	try {
	    if(request.isAnonymous()) {
		String json = JsonUtil.map(Utils.makeList("error", JsonUtil.quote("You must be logged in to use the rewrite service")));
		return new Result("", new StringBuilder(json), "text/json");
	    }

	    String text =request.getString("text","");
	    String promptPrefix = request.getString("promptprefix",
						    "Rewrite the following text as college level material:");
	    String promptSuffix = request.getString("promptsuffix", "");
	    //	text = callGpt("Rewrite the following text:","",new StringBuilder(text),1000,false);		    
	    text = callLLM(request, promptPrefix,promptSuffix,text,1000,false,null);		    
	    String json = JsonUtil.map(Utils.makeList("result", JsonUtil.quote(text)));
	    return new Result("", new StringBuilder(json), "text/json");
	} catch(Throwable exc) {
	    throw new RuntimeException(exc);
	}
	
    }

    public String applyPromptToDocument(Request request, File file, String prompt,int offset) throws Exception {
	try {
	    String corpus = getSearchManager().extractCorpus(request, file, null);
	    if(corpus==null) return null;
	    if(offset>0 && offset<corpus.length()) {
		corpus = corpus.substring(offset);
	    }
	    return callLLM(request, prompt,"",corpus,1000,true, new int[]{TOKEN_LIMIT_GPT3});
	} catch(Throwable exc) {
	    throw new RuntimeException(exc);
	}

    }


    private Result makeJsonErrorResult(String error) {
	String json = JsonUtil.map(Utils.makeList("error", JsonUtil.quote(error)));
	return new Result("", new StringBuilder(json), "text/json");
    }



    private IO.Result call(JobManager jobManager, final URL url, final String body, final String...args) 
	throws Exception {
	final IO.Result[] theResult={null};
	Callable<Boolean> callable = new Callable<Boolean>() {
		public Boolean call() {
		    try {
			theResult[0] = IO.getHttpResult(IO.HTTP_METHOD_POST,url,body,args);
		    } catch (Exception exc) {
			throw new RuntimeException(exc);
		    }
		    return Boolean.TRUE;
		}};
	try {
	    jobManager.invokeAndWait(callable);
	} catch(Throwable thr) {
	    throw new RuntimeException(thr);
	}
	return theResult[0];
    }


    public IO.Result  callGemini(String model, String gptText) throws Exception {
	synchronized(MUTEX_GEMINI) {
	    if(!isGeminiEnabled()) return null;
	    /*{
	      "contents": [{
	      "parts":[{"text": "Write a story about a magic backpack."}]}]}'
	    */
	    String geminiKey = getRepository().getProperty(PROP_GEMINI_KEY);	
	    String contents = JU.list(JU.map("parts",JU.list(JU.map("text",JU.quote(gptText)))));
	    String body = JU.map("contents",contents);
	    //		System.err.println(body);
	    return call(geminiJobManager,
			new URL(URL_GEMINI+"?key=" + geminiKey), body,
			"Content-Type","application/json");
	}
    }	



    private  IO.Result  callOpenAI(Request request, String model, int maxReturnTokens,String gptText,String[]extraArgs)
	throws Throwable {
	if(!isOpenAIEnabled()) return null;
	//	synchronized(MUTEX_OPENAI) {
	boolean useGpt4 = model.equals(MODEL_GPT_4);
	if(useGpt4 && !isGPT4Enabled()) return null;
	List<String> args =  Utils.makeList("temperature", "0",
					    "max_tokens" ,""+ maxReturnTokens,
					    "top_p", "1.0");
	for(int i=0;i<extraArgs.length;i+=2) {
	    args.add(extraArgs[i]);
	    args.add(extraArgs[i+1]);
	}
	Utils.add(args,"model",JsonUtil.quote(model));
	Utils.add(args,"messages",JsonUtil.list(JsonUtil.map(
							     "role",JsonUtil.quote("user"),
							     "content",JsonUtil.quote(gptText))));
	String body = JsonUtil.map(args);
	String openAIKey = getOpenAIKey();
	IO.Result result=call(openAIJobManager,new URL(URL_OPENAI_COMPLETION), body,
		    "Content-Type","application/json",
		    "Authorization","Bearer " +openAIKey);
	if(result!=null) {
	    String remTokens = result.getHeader("x-ratelimit-remaining-tokens");
	    String remRequests = result.getHeader("x-ratelimit-remaining-requests");	    
	    String resetTokens = result.getHeader("x-ratelimit-reset-tokens");
	    String resetRequests = result.getHeader("x-ratelimit-reset-requests");	    
	    int tokens = remTokens!=null?Integer.parseInt(remTokens):1000000;
	    int requests = remRequests!=null?Integer.parseInt(remRequests):1000000;
	    //	    System.err.println("TOKENS:" + tokens +" REQUESTS:" + requests +" reset tokens:" + resetTokens +" reset requests:" + resetRequests);
	}

	return result;
    }




    public String callLLM(Request request,
			  String prompt1,
			  String prompt2,
			  String text,
			  int maxReturnTokens,
			  boolean tokenize,
			  int[]initTokenLimit,
			  String...extraArgs)
	throws Throwable {
	String model = request.getString(ARG_MODEL,MODEL_GPT_3_5);
	String openAIKey = getOpenAIKey();
	String geminiKey = getRepository().getProperty(PROP_GEMINI_KEY);	
	if(openAIKey==null && geminiKey==null) {
	    if(debug) System.err.println("\tno LLM key");
	    return null;
	}

	if(initTokenLimit==null) 
	    initTokenLimit = new int[]{-1};

	if(initTokenLimit[0]<=0) {
	    if(model.equals(MODEL_GEMINI)) 
		initTokenLimit[0] = TOKEN_LIMIT_GEMINI;
	    else if(model.equals(MODEL_GPT_4)) 
		initTokenLimit[0] = TOKEN_LIMIT_GPT4;
	    else
		initTokenLimit[0] = TOKEN_LIMIT_GPT3;
	} 

	int callCnt = 0;
	int tokenLimit = initTokenLimit[0];
	while(tokenLimit>=500) {
	    initTokenLimit[0] = tokenLimit;
	    StringBuilder gptCorpus = new StringBuilder(prompt1);
	    gptCorpus.append("\n\n");
	    if(!tokenize) {
		gptCorpus.append(text);
	    } else {
		text = Utils.removeNonAscii(text," ").replaceAll("[,-\\.\n]+"," ").replaceAll("  +"," ");
		if(text.trim().length()==0) {
		    if(debug) getLogManager().logSpecial("LLMManager.callGpt no text");
		    return null;
		}
		List<String> toks = Utils.split(text," ",true,true);
		//best guess-
		int extraCnt = 0;
		int i =0;
		for(i=0;i<toks.size() && i+extraCnt<tokenLimit;i++) {
		    String tok = toks.get(i);
		    if(tok.length()>5) {
			extraCnt+=(int)(tok.length()/5);
		    }
		    gptCorpus.append(tok);
		    gptCorpus.append(" ");
		}
	    }
	    gptCorpus.append("\n\n");
	    gptCorpus.append(prompt2);
	    String gptText =  gptCorpus.toString();
	    gptCorpus=null;
	    if(debug) getLogManager().logSpecial("LLMManager: model:" + model +
						 " init length:" + text.length() +
						 " text length:" + gptText.length() +
						 " max return tokens:" + maxReturnTokens +
						 " token limit:" + initTokenLimit[0]);

	    IO.Result  result=null; 
	    if(model.equals(MODEL_GEMINI)) {
		long t1 = System.currentTimeMillis();
		result = callGemini(model,gptText);
		long t2 = System.currentTimeMillis();
		//		if(debug)getLogManager().logSpecial("LLMManager:done calling Gemini:" + (t2-t1)+"ms");
	    } else if(Utils.equalsOne(model,MODEL_GPT_4,MODEL_GPT_3_5)) {
		long t1 = System.currentTimeMillis();
		result = callOpenAI(request,model,maxReturnTokens,gptText,extraArgs);
		//		System.err.println(result.getResult());
		long t2 = System.currentTimeMillis();
		//		if(debug)getLogManager().logSpecial("LLMManager:done calling OpenAI:" + (t2-t1)+"ms");
	    }
	    if(result==null) {
		if(debug)
		    getLogManager().logSpecial("\tno result for model:" + model);
		return null;
	    }

	    if(result.getError()) {
		try {
		    JSONObject json = new JSONObject(result.getResult());
		    JSONObject error = json.optJSONObject("error");
		    if(error == null) {
			System.err.println("Error calling LLM. No error in result:" + result.getResult());
			return null;
		    }

		    String code = error.optString("code",null);
		    if(code == null) {
			System.err.println("LLMManager Error: No code in result:" + result.getResult());
			return null;
		    }

		    if(code.equals("rate_limit_exceeded")) {
			//https://platform.openai.com/docs/guides/rate-limits/usage-tiers?context=tier-two
			long ms= 1000*60;
			String delay = StringUtil.findPattern(result.getResult(),"Please try again in ([\\d\\.]+)s");
			if(delay!=null) {
			    try {ms = (int)(1000*1.5*Double.parseDouble(delay));} catch(Exception ignore){}
			} else {
			    delay = StringUtil.findPattern(result.getResult(),"Please try again in ([\\d\\.]+)ms");
			    if(delay!=null) {
				try {ms = (int)(1.5*Double.parseDouble(delay));} catch(Exception ignore){}
			    }
			}
			ms = (long)(ms*Math.pow(2, callCnt));
			callCnt++;
			if(callCnt>10) return null;
			getLogManager().logSpecial("LLMManager: rate limit exceeded."+
						   " call cnt:"+ callCnt +
						   " sleeping for:" +  ms+" ms");

			Misc.sleep(ms);
			tokenLimit-=1000;
			continue;
		    }

		    if(!code.equals("context_length_exceeded")) {
			System.err.println("\tLLMManager: Error calling LLM. Unknown error code:" + result.getResult());
			return null;
		    }
		    tokenLimit-=1000;
		    if(debug)
			System.err.println("\ttoo many tokens. Trying again with limit:" + tokenLimit);
		    continue;
		} catch(Exception exc) {
		    exc.printStackTrace();
		    throw new RuntimeException("Unable to process GPT request:" + result.getResult());
		}
	    }

	    JSONObject json = new JSONObject(result.getResult());
	    if(debug)	{
		String tokens = JsonUtil.readValue(json,"usage.prompt_tokens",null);
		if(tokens!=null) {
		    //		    getLogManager().logSpecial("OpenAI: prompt tokens:" + tokens+
		    //					       " completion tokens:" + JsonUtil.readValue(json,"usage.completion_tokens","NA"));
		} else {
		    //		    System.err.println("LLMManager: json: "  +Utils.clip(result.getResult().replace("\n"," ").replaceAll("  +"," "),200,"..."));
		}
	    }
	    //Google PALM
	    if(json.has("candidates")) {
		/*{
		  "candidates": [
		  {
		  "content": {
		  "parts": [
		  {
		  "text": "AN ANCIENT ROCKY MOUNTAIN CAVER"
		  }
		  ],
		  "role": "model"
		  },
		*/
		JSONArray candidates = json.getJSONArray("candidates");
		if(candidates.length()==0) return null;
		JSONObject candidate= candidates.getJSONObject(0);
		JSONArray parts = candidate.getJSONObject("content").getJSONArray("parts");
		String t = parts.getJSONObject(0).optString("text",null);
		return t;
	    }

	    if(json.has("choices")) {
		JSONArray choices = json.getJSONArray("choices");
		if(choices.length()>0) {
		    JSONObject choice= choices.getJSONObject(0);
		    if(choice.has("text")) {
			return choice.getString("text");
		    } else if(choice.has("message")) {
			JSONObject message= choice.getJSONObject("message");
			return message.optString("content",null);
		    }
		    System.err.println("\tLLMManager:No results from GPT:" + result);
		}
	    }
	}
	return null;
    }

    public Result processTranscribe(Request request)  throws Exception {
	try {
	    return processTranscribeInner(request);
	} catch(Exception exc) {
	    getLogManager().logError("Error calling transcribe",exc);
	    return makeJsonErrorResult("An error has occurred:" + exc);
	}
    }


    private Result processTranscribeInner(Request request)  throws Exception {	
	if(request.isAnonymous()) {
	    return makeJsonErrorResult("You must be logged in to use the rewrite service");
	}

	String openAIKey = getOpenAIKey();
	if(openAIKey==null) {
	    if(debug) System.err.println("\tno OpenAI key");
	    return makeJsonErrorResult("Transcription processing is not enabled");
	}

	File file = new File(request.getUploadedFile("audio-file"));
	String[]args = new String[]{"Authorization","Bearer " +openAIKey};
	String mime = request.getString("mimetype","audio/webm");

	String fileName = "audio" + mime.replaceAll(".*/",".");
	//	fileName = "test.mp4";
	//	file = new File("/Users/jeffmc/test.mp4");
	//	mime = "audio/mp4";

	List postArgs   =Utils.add(new ArrayList(),
				   "model",MODEL_WHISPER_1,"file", new IO.FileWrapper(file,fileName,mime));

	URL url =  new URL(URL_OPENAI_TRANSCRIPTION);
	IO.Result result =  IO.doMultipartPost(url, args,postArgs);
	if(debug) System.err.println("LLMManager.transcribe:" + result);
	String results = result.getResult();
	JSONObject json = new JSONObject(results);
	if(json.has("text")) {
	    String text = json.getString("text");
	    if(request.get("addfile",false)) {
		getEntryManager().processEntryAddFile(request, file,fileName,text);
	    }

	    StringBuilder sb = new StringBuilder(JsonUtil.map(Utils.makeList("results", JsonUtil.quote(text))));
	    return new Result("", sb, "text/json");
	}
	if(json.has("error")) {
	    JSONObject error = json.getJSONObject("error");
	    return makeJsonErrorResult(error.getString("message"));
	}
	return makeJsonErrorResult("An error occurred:" + results);
    }





    public static final String PROMPT_JSON = "Summarize the below text, extracting out the title, a summary to be no longer than four sentences, a list of keywords (limited to no more than 6 keywords), and if you can a list of authors. Your result must be valid JSON following the form:\n{\"title\":<the title>,\"authors\":<the authors>,\"summary\":<the summary>,\"keywords\":<the keywords>\n}\n";

    public boolean applyEntryExtract(final Request request, final Entry entry, final String llmCorpus) throws Throwable {
	if(true) return applyEntryExtractInner(request, entry,llmCorpus);
	//for testing:
	for(int i=0;i<40;i++) {
	    Misc.run(new Runnable(){
		    public void run() {
			try {
			    applyEntryExtractInner(request, entry,llmCorpus);
			}catch(Throwable thr) {
			    System.err.println("ERROR:" + thr);
			}
		    }
		});
	}
	return true;
    }

    public boolean applyEntryExtractInner(final Request request, final Entry entry, final String llmCorpus) throws Throwable {	


	boolean entryChanged = false;
	int[]tokenLimit = new int[]{-1};

	boolean extractKeywords = request.get(ARG_EXTRACT_KEYWORDS,false);
	boolean extractSummary = request.get(ARG_EXTRACT_SUMMARY,false);	
	boolean extractTitle = request.get(ARG_EXTRACT_TITLE,false);	
	boolean extractAuthors = request.get(ARG_EXTRACT_AUTHORS,false);	

	if(!(extractKeywords || extractSummary || extractTitle || extractAuthors)) return false;
	String jsonPrompt= "You are a skilled document editor and I want you to extract the following information from the given text. The text is a document. Assume the reader has a college education. The response must be in valid JSON format and only JSON.";
	List<String> schema =new ArrayList<String>();
	if(extractTitle) {
	    jsonPrompt+="You should include a title. ";
	    schema.add("\"title\":\"<the title>\"");
	}
	if(extractSummary) {
	    jsonPrompt+="You should include a paragraph summary of the text. The summary should be around four lines. The result in the JSON should be a JSON string. ";
	    schema.add("\"summary\":\"<the summary>\"");
	}
	if(extractKeywords) {
	    jsonPrompt+="You must include a list of keywords extracted from the document. The keywords should not include anyone's name and should accurately capture the important details of the document. Furthermore, each keyword should not contain more than 3 separate words. The keywords must be returned as a valid JSON list of strings. There should be no more than 8 keywords in the list. ";
	    schema.add("\"keywords\":[<keywords>]");
	}
	if(extractAuthors) {
	    jsonPrompt+="You should include a list of authors of the text. The authors should be a valid JSON list of strings. ";
	    schema.add("\"authors\":[<authors>]");
	}
	jsonPrompt +="\nThe result JSON must adhere to the following schema: \n{" + Utils.join(schema,",")+"}\n";
	//	System.err.println("Prompt:" + jsonPrompt);
	String json = callLLM(request, jsonPrompt+"\nThe text:\n","",llmCorpus,200,true,tokenLimit);
	if(!stringDefined(json)) {
	    getLogManager().logSpecial("LLMManager:Failed to extract information for entry:" + entry.getName());
	    return false;
	}

	//	    getLogManager().logSpecial("LLMManager:success:" + entry.getName());
	json = json.replaceAll("^```json","").replaceAll("```$","").trim();
	//	    System.err.println("JSON:" + json);
	try {
	    JSONObject obj = new JSONObject(json);
	    if(extractTitle) {
		String title = obj.optString("title",null);
		title = title.trim().replaceAll("\"","").replaceAll("\"","");
		entry.setName(title);
	    }
	    if(extractSummary) {
		String summary = obj.optString("summary",null);
		if(summary!=null) {
		    summary = Utils.stripTags(summary).trim().replaceAll("^:+","");
		    StringBuilder sb = new StringBuilder();
		    for(String line:Utils.split(summary,"\n",true,true)) {
			line = line.replaceAll("^-","");
			sb.append(line);
			sb.append("\n");
		    }
		    summary = "+toggleopen Summary\n+callout-info\n" + sb+"\n-callout-info\n-toggle\n";
		    entryChanged = true;
		    entry.setDescription(summary+"\n"+entry.getDescription());
		}
	    }
	    if(extractKeywords) {
		JSONArray array = obj.optJSONArray("keywords");
		//		    System.err.println("model:" +request.getString(ARG_MODEL,MODEL_GPT_3_5)+" keywords:" + array);
		if(array!=null) {
		    for (int i = 0; i < array.length(); i++) {
			if(i>=8) break;
			getMetadataManager().addKeyword(request, entry, array.getString(i));
		    }
		}
	    }
	    if(extractAuthors) {
		JSONArray array = obj.optJSONArray("authors");
		if(array!=null) {
		    for (int i = 0; i < array.length(); i++) {
			if(i>=10) break;
			getMetadataManager().addMetadata(request, entry,
							 "metadata_author", MetadataManager.CHECK_UNIQUE_TRUE,
							 array.getString(i));
		    }
		}
	    }
	    return true;
	} catch(Exception exc) {
	    getLogManager().logSpecial("LLMManager:Error parsing JSON:" + exc+" json:" + json);
	    exc.printStackTrace();
	}
	return false;


    }




    private static class WordCount {
	int count=0;
	String word;
	WordCount(String word) {
	    this.word = word;
	}
	public String toString() {
	    return word+" #:" + count +" ";
	}
    }

}


