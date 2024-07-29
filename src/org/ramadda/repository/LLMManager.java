/**
   Copyright (c) 2008-2024 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.repository;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.admin.*;
import org.ramadda.repository.metadata.MetadataManager;
import static org.ramadda.repository.type.TypeHandler.CorpusType;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;

import ucar.unidata.util.LogUtil;
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
    public static final String PROP_CLAUDE_KEY = "claude.api.key";	    
    

    public static final int TOKEN_LIMIT_UNDEFINED = -1;
    public static final int TOKEN_LIMIT_GEMINI = 30000;
    public static final int TOKEN_LIMIT_GPT3 = 2000;    
    public static final int TOKEN_LIMIT_GPT4 = 4000;
    public static final int TOKEN_LIMIT_CLAUDE = 200000;

    private static final Object MUTEX_GEMINI = new Object();
    private static final Object MUTEX_CLAUDE = new Object();
    private static final Object MUTEX_OPENAI = new Object();    

    public static final String MODEL_WHISPER_1 = "whisper-1";
    public static final String MODEL_GPT_3_5="gpt-3.5-turbo-1106";
    public static final String MODEL_GPT_4="gpt-4";
    public static final String MODEL_GPT_VISION  = "gpt-4-vision-preview";
    public static final String MODEL_GEMINI = "gemini";
    public static final String MODEL_CLAUDE = "claude";    

    public static final String ARG_USEGPT4  = "usegpt4";
    public static final String ARG_MODEL = "model";

    public static final String ARG_EXTRACT_KEYWORDS = "extract_keywords";
    public static final String ARG_EXTRACT_SUMMARY = "extract_summary";    

    public static final String ARG_EXTRACT_AUTHORS = "extract_authors";
    public static final String ARG_EXTRACT_TITLE = "extract_title";	        
    public static final String ARG_EXTRACT_LOCATIONS = "extract_locations";
    public static final String ARG_EXTRACT_LATLON = "extract_latlon";


    public static final String URL_OPENAI_TRANSCRIPTION = "https://api.openai.com/v1/audio/transcriptions";
    public static final String URL_OPENAI_COMPLETION =  "https://api.openai.com/v1/chat/completions";
    public static final String URL_GEMINI="https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    public static final String URL_CLAUDE="https://api.anthropic.com/v1/messages";



    private JobManager openAIJobManager;
    private JobManager geminiJobManager;
    private JobManager claudeJobManager;        

    private Object NEXT_MUTEX= new Object();

    public LLMManager(Repository repository) {
        super(repository);
	openAIJobManager = new JobManager(repository,
					  repository.getProperty("ramadda.llm.openai.threads",2));
	//	openAIJobManager.setDebug(true);
	geminiJobManager = new JobManager(repository,
					  repository.getProperty("ramadda.llm.gemini.threads",2));
	claudeJobManager = new JobManager(repository,
					  repository.getProperty("ramadda.llm.clause.threads",2));	
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
    private boolean isClaudeEnabled() {
	return  Utils.stringDefined(getRepository().getProperty(PROP_CLAUDE_KEY));
    }    


    private boolean isOpenAIEnabled() {
	return Utils.stringDefined(getRepository().getProperty(PROP_OPENAI_KEY));
    }


    public boolean isGPT4Enabled() {
	return getRepository().getProperty("openai.gpt4.enabled",false);
    }    

    
    public boolean isLLMEnabled() {
	return isGeminiEnabled() || isOpenAIEnabled() || isClaudeEnabled();
    }


    public void processArgs(Request request, String...args) {
	for(String a:args) {
	    if(a.equals("title")) request.put(LLMManager.ARG_EXTRACT_TITLE,"true");
	    else if(a.equals("summary")) request.put(LLMManager.ARG_EXTRACT_SUMMARY,"true");
	    else if(a.equals("authors")) request.put(LLMManager.ARG_EXTRACT_AUTHORS,"true");
	    else if(a.equals("locations")) request.put(LLMManager.ARG_EXTRACT_LOCATIONS,"true");	    
	    else if(a.equals("latlon")) request.put(LLMManager.ARG_EXTRACT_LATLON,"true");	    
	    else if(a.equals("keywords")) request.put(LLMManager.ARG_EXTRACT_KEYWORDS,"true");
	    else if(a.equals("debug")) request.put("debug","true");
	    else if(a.startsWith("model:")) {
		String model = a.substring("model:".length());
		setModel(request,model);

	    }
	}
    }


    public void setModel(Request request, String model) {
	if(model.equals("gpt3.5")) model=MODEL_GPT_3_5;
	else if(model.equals("gpt4")) model=MODEL_GPT_4;
	request.put(ARG_MODEL,model);
    }


    public Result applyLLM(Request request, Entry entry) throws Exception  {
        StringBuilder sb      = new StringBuilder();
	String pageUrl =request.toString();
	String subLabel = HU.href(pageUrl,"Apply LLM",HU.cssClass("ramadda-clickable"));
	getPageHandler().entrySectionOpen(request, entry, sb, subLabel);
	if(!getAccessManager().canDoEdit(request, entry)) {
            sb.append(getPageHandler().showDialogError(
						       "You are not allowed to edit the document"));
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return new Result("Error",sb);
	}

	if(request.exists(ARG_OK)) {
	    applyLLMToEntry(request, entry,sb);
	} else {
	    sb.append(HU.formPost(pageUrl,""));
	    sb.append(getNewEntryExtract(request));
	    sb.append("<br>");
	    sb.append(HU.submit("Apply LLM", ARG_OK));
	    sb.append(HU.formClose());
	}
	getPageHandler().entrySectionClose(request, entry, sb);
	return getEntryManager().addEntryHeader(request, entry,
						new Result("Apply LLM", sb));
    }

    public boolean applyLLMToEntry(Request request, Entry entry,StringBuilder sb) throws Exception {
	String corpus = entry.getTypeHandler().getCorpus(request, entry,CorpusType.SEARCH);
	if(corpus==null) {
	    sb.append(getPageHandler().showDialogError("No text from file available."));
	    return false;
	} else {
	    applyEntryExtract(request, entry, corpus);
	    getEntryManager().updateEntry(request, entry);
	    sb.append(getPageHandler().showDialogNote("LLM has been applied."));
	    return true;
	}
    }


    private List<HtmlUtils.Selector> getAvailableModels() {
	List<HtmlUtils.Selector> models = new ArrayList<HtmlUtils.Selector>();
	if(isOpenAIEnabled()) {
	    models.add(new HtmlUtils.Selector("OpenAI GPT3.5",MODEL_GPT_3_5));
	    if(isGPT4Enabled()) {
		models.add(new HtmlUtils.Selector("OpenAI GPT4.0",MODEL_GPT_4));
	    }
	}
	if(isGeminiEnabled()) {
	    models.add(new HtmlUtils.Selector("Google Gemini",MODEL_GEMINI));
	}
	if(isClaudeEnabled()) {
	    models.add(new HtmlUtils.Selector("Claude",MODEL_CLAUDE));
	}	
	return models;
    }


    public String getNewEntryExtract(Request request) {
	if(!isLLMEnabled()) return "";
	String space = HU.space(3);
	StringBuilder sb = new StringBuilder();
	List<HtmlUtils.Selector> models = getAvailableModels();
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
	HU.labeledCheckbox(sb, ARG_EXTRACT_LOCATIONS, "true", request.get(ARG_EXTRACT_LOCATIONS,false),"","Extract named geographic locations");
	sb.append(space);
	HU.labeledCheckbox(sb, ARG_EXTRACT_LATLON, "true", request.get(ARG_EXTRACT_LATLON,false),"","Extract latitude/longitude");		
	sb.append("<br>");

	getWikiManager().makeCallout(sb,request,"When extracting keywords, title, etc., the file text is sent to the selected LLM (e.g., <a href=https://openai.com/api/>OpenAI GPT API</a>) for processing.<br>There will also be a delay before the results are shown for the new entry.");
	return sb.toString();
    }



    public Result processLLM(Request request)  throws Exception {
	try {
	    if(request.isAnonymous()) {
		String json = JsonUtil.map(Utils.makeListFromValues("error", JsonUtil.quote("You must be logged in to use the rewrite service")));
		return new Result("", new StringBuilder(json), "text/json");
	    }

	    String text =request.getString("text","");
	    String promptPrefix = request.getString("promptprefix",
						    "Rewrite the following text as college level material:");
	    String promptSuffix = request.getString("promptsuffix", "");
	    text = callLLM(request, promptPrefix,promptSuffix,text,1000,false,new PromptInfo() );
	    String json = JsonUtil.map(Utils.makeListFromValues("result", JsonUtil.quote(text)));
	    return new Result("", new StringBuilder(json), "text/json");
	} catch(Throwable exc) {
	    throw new RuntimeException(exc);
	}
	
    }


    public String applyPromptToDocument(Request request, Entry entry, boolean document, String prompt,PromptInfo info)
	throws Exception {
	try {
	    if(info==null) info=new PromptInfo();
	    String corpus = null;
	    if(document) {
		corpus = entry.getTypeHandler().getCorpus(request, entry,CorpusType.LLM);
	    } else {
		StringBuilder sb = new StringBuilder();
		
		sb.append("The below text is JSON describing a " + entry.getTypeHandler().getDescription()+"\n");
		String typePrompt = entry.getTypeHandler().getTypeProperty("llm.prompt",(String) null);
		if(typePrompt!=null) {
		    sb.append(typePrompt);
		    sb.append("\n");
		}
		sb.append("\nIn your response do not mention in any way that the source document is JSON. I repeat do not mention anything about JSON.\n");
		List<Entry> entries= new ArrayList<Entry>();
		entries.add(entry);
		getRepository().getJsonOutputHandler().makeJson(request,entries,sb);
		corpus=sb.toString();
	    }
	    if(corpus==null) return null;
	    info.corpusLength=corpus.length();
	    if(info.offset>0 && info.offset<corpus.length()) {
		corpus = corpus.substring(info.offset);
	    }
	    //	    info.tokenLimit = TOKEN_LIMIT_GPT3;
	    return callLLM(request, prompt,"",corpus,1000,true, info);
	} catch(Throwable exc) {
	    throw new RuntimeException(exc);
	}

    }


    private Result makeJsonErrorResult(String error) {
	String json = JsonUtil.map(Utils.makeListFromValues("error", JsonUtil.quote(error)));
	return new Result("", new StringBuilder(json), "text/json");
    }



    private IO.Result call(JobManager jobManager, final URL url, final String body, final String...args) 
	throws Exception {
	//	System.err.println("URL:" + url);
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

    public IO.Result  callClaude(String model, String gptText) throws Exception {
	synchronized(MUTEX_CLAUDE) {
	    if(!isClaudeEnabled()) return null;
	    /*
	      '{
    "model": "claude-3-opus-20240229",
    "max_tokens": 1024,
    "messages": [
        {"role": "user", "content": "Hello, world"}
    ]
}'
	    */
	    /*
	    gptText = gptText +"<document>" +
		IO.readContents("/Users/jeffmc/test.csv")+"</document>";
	    System.err.println(gptText);
	    */



	    String claudeKey = getRepository().getProperty(PROP_CLAUDE_KEY);	
	    String messages = JU.list(JU.map("role",JU.quote("user"),"content",JU.quote(gptText)));
	    String body = JU.map("model",JU.quote("claude-3-sonnet-20240229"),
				 "max_tokens","1024",
				 "messages",messages);

	    return call(claudeJobManager,
			new URL(URL_CLAUDE+"?key=" + claudeKey), body,
			"x-api-key",claudeKey,
			"anthropic-version","2023-06-01",
			"Content-Type","application/json");
	}
    }	
    


    private  IO.Result  callOpenAI(Request request, String model,
				   int maxReturnTokens,
				   String gptText,String[]extraArgs)
	throws Throwable {
	if(!isOpenAIEnabled()) return null;
	//	synchronized(MUTEX_OPENAI) {
	boolean useGpt4 = model.equals(MODEL_GPT_4);
	if(useGpt4 && !isGPT4Enabled()) return null;
	List<String> args =  Utils.makeListFromValues("temperature", "0",
					    "max_tokens" ,""+ maxReturnTokens,
					    "top_p", "1.0");
	for(int i=0;i<extraArgs.length;i+=2) {
	    args.add(extraArgs[i]);
	    args.add(extraArgs[i+1]);
	}
	Utils.add(args,"model",JsonUtil.quote(model));
	System.err.println(gptText.length());
	Utils.add(args,"messages",JsonUtil.list(JsonUtil.map(
							     "role",JsonUtil.quote("user"),
							     "content",JsonUtil.quote(gptText))));
	String body = JsonUtil.map(args);
	String openAIKey = getOpenAIKey();
	IO.Result result=call(openAIJobManager,new URL(URL_OPENAI_COMPLETION), body,
		    "Content-Type","application/json",
		    "Authorization","Bearer " +openAIKey);
	//	System.err.println(result.getHeaders());
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




    private String callLLM(Request request,
			   String prompt1,
			   String prompt2,
			   String text,
			   int maxReturnTokens,
			   boolean tokenize,
			   PromptInfo info,
			   String...extraArgs)
	throws Throwable {
	String model = request.getString(ARG_MODEL,MODEL_GPT_3_5);
	String openAIKey = getOpenAIKey();
	String geminiKey = getRepository().getProperty(PROP_GEMINI_KEY);	
	if(openAIKey==null && geminiKey==null) {
	    if(debug) System.err.println("\tno LLM key");
	    return null;
	}

	if(info.tokenLimit<=0) {
	    if(model.equals(MODEL_GEMINI)) 
		info.tokenLimit = TOKEN_LIMIT_GEMINI;
	    else if(model.equals(MODEL_GPT_4)) 
		info.tokenLimit = TOKEN_LIMIT_GPT4;
	    else if(model.equals(MODEL_CLAUDE)) 
		info.tokenLimit = TOKEN_LIMIT_CLAUDE;	    
	    else
		info.tokenLimit = TOKEN_LIMIT_GPT3;
	} 

	int callCnt = 0;
	int tokenLimit = info.tokenLimit;
	info.segmentLength=0;
	while(tokenLimit>=500) {
	    info.tokenLimit = tokenLimit;
	    StringBuilder gptCorpus = new StringBuilder(prompt1);
	    gptCorpus.append("\n\n");
	    if(!tokenize) {
		gptCorpus.append(text);
		info.segmentLength+=text.length();
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
		    info.segmentLength+=tok.length();
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
						 " token limit:" + info.tokenLimit);

	    IO.Result  result=null; 
	    if(model.equals(MODEL_GEMINI)) {
		long t1 = System.currentTimeMillis();
		result = callGemini(model,gptText);
		long t2 = System.currentTimeMillis();
		//		if(debug)getLogManager().logSpecial("LLMManager:done calling Gemini:" + (t2-t1)+"ms");
	    } else if(model.equals(MODEL_CLAUDE)) {
		long t1 = System.currentTimeMillis();
		result = callClaude(model,gptText);
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

	    //	    System.err.println("RESULT:" + result.getResult());
	    if(result.getError()) {
		if(model.equals(MODEL_CLAUDE)) {
		    if(result.getCode()==529) {
			throw new CallException("Claude' API is temporarily overloaded.");
		    }
		    if(result.getCode()==429) {
			throw new CallException("Claude's API rate limit has been exceeded.");
		    }
		    if(result.getCode()==400) {
			throw new CallException("The call to the Claude API as malformed.");
		    }
		    if(result.getCode()==401) {
			throw new CallException("The call to the Claude API had an authentication error.");
		    }		    		    
		    throw new CallException("Some error occurred calling the Claude API:" + result.getCode());
		}

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
			info.tokenLimit-=1000;
			continue;
		    }

		    if(!code.equals("context_length_exceeded")) {
			System.err.println("\tLLMManager: Error calling LLM. Unknown error code:" + result.getResult());
			return null;
		    }
		    info.tokenLimit-=1000;
		    if(debug)
			System.err.println("\ttoo many tokens. Trying again with limit:" + info.tokenLimit);
		    continue;
		} catch(Exception exc) {
		    //		    exc.printStackTrace();
		    String msg = result.getResult();
		    if(result.getCode()==429)   msg = "Too many requests";
		    throw new RuntimeException("Unable to process GPT request:" + msg);
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

	    if(json.has("content")) {
		JSONArray choices = json.getJSONArray("content");
		if(choices.length()>0) {
		    JSONObject choice= choices.getJSONObject(0);
		    if(choice.has("text")) {
			return choice.getString("text");
		    }
		    System.err.println("\tLLMManager:No results from GPT:" + result);
		}
		throw new CallException("No results from GPT call");
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
	} catch(Throwable exc) {
	    getLogManager().logError("Error calling transcribe",exc);
	    return makeJsonErrorResult("An error has occurred:" + exc);
	}
    }


    private Result processTranscribeInner(Request request)  throws Throwable {	
	boolean debug = request.get("debug",this.debug);
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
	    if(request.get("sendtochat",false)) {
		text = callLLM(request, "","",text,2000,true,new PromptInfo());
	    }
	    if(request.get("addfile",false)) {
		getEntryManager().processEntryAddFile(request, file,fileName,text);
	    }


	    StringBuilder sb = new StringBuilder(JsonUtil.map(Utils.makeListFromValues("results", JsonUtil.quote(text))));
	    return new Result("", sb, "text/json");
	}
	if(json.has("error")) {
	    JSONObject error = json.getJSONObject("error");
	    return makeJsonErrorResult(error.getString("message"));
	}
	return makeJsonErrorResult("An error occurred:" + results);
    }







    public boolean applyEntryExtract(final Request request, final Entry entry, final String llmCorpus)
	throws Exception {
	if(true) {
	    try {
		return applyEntryExtractInner(request, entry,llmCorpus);
	    } catch(CallException exc) {
		getSessionManager().addSessionErrorMessage(request,"Error doing LLM extraction:" + entry+" " + exc.getMessage());
		return false;
	    } catch(Throwable thr) {
		getSessionManager().addSessionErrorMessage(request,"Error doing LLM extraction:" + entry+" " + thr.getMessage());
		throw new RuntimeException(thr);
	    } finally {
		getEntryManager().clearEntryState(entry,"message");
	    }
	}
	    
	//for testing:
	/*
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
		}*/
	return true;
    }

    public boolean applyEntryExtractInner(final Request request, final Entry entry, final String llmCorpus) throws Throwable {	


	boolean entryChanged = false;
	PromptInfo info = new PromptInfo();
	boolean extractKeywords = request.get(ARG_EXTRACT_KEYWORDS,false);
	boolean extractSummary = request.get(ARG_EXTRACT_SUMMARY,false);	
	boolean extractTitle = request.get(ARG_EXTRACT_TITLE,false);	
	boolean extractAuthors = request.get(ARG_EXTRACT_AUTHORS,false);
	boolean extractLocations = request.get(ARG_EXTRACT_LOCATIONS,false);
	boolean extractLatLon = request.get(ARG_EXTRACT_LATLON,false);				
	if(!(extractKeywords || extractSummary || extractTitle || extractAuthors||extractLocations||extractLatLon)) return false;
	getEntryManager().putEntryState(entry,"message","Performing LLM extraction");


	String jsonPrompt= "You are a skilled document editor and I want you to extract the following information from the given text. Assume the reader of your result has a college education. The text is a document. ";
	String typePrompt = entry.getTypeHandler().getTypeProperty("llm.prompt",(String) null);
	if(typePrompt!=null) {
	    jsonPrompt=typePrompt;
	    jsonPrompt+="\n";
	}
	jsonPrompt+="The name of the original document file is: " +getStorageManager().getOriginalFilename(entry.getResource().getPathName())+".\n";

	jsonPrompt+="The type of the document is: " + entry.getTypeHandler().getDescription()+".\n";

	typePrompt = entry.getTypeHandler().getTypeProperty("llm.prompt.extra",(String) null);
	if(typePrompt!=null) {
	    jsonPrompt+=typePrompt+"\n";
	}	
	jsonPrompt+="The response must be in valid JSON format and only JSON. I reiterate, the result must only be valid JSON. Make sure that any embedded strings are properly escaped.\n";

	List<String> schema =new ArrayList<String>();
	if(extractTitle) {
	    jsonPrompt+="You should include a title. ";
	    schema.add("\"title\":\"<the title>\"");
	}
	if(extractSummary) {
	    jsonPrompt+="You must include a paragraph summary of the text. The summary must not in any way be longer than four sentences. Each sentence should be short and to the point. The result in the JSON must, and I mean must, be a valid JSON string. ";
	    schema.add("\"summary\":\"<the summary>\"");
	}
	if(extractKeywords) {
	    jsonPrompt+="You must include a list of keywords extracted from the document. The keywords should not include anyone's name and should accurately capture the important details of the document. Furthermore, each keyword should not contain more than 3 separate words. The keywords must be returned as a valid JSON list of strings. There should be no more than 4 keywords in the list. ";
	    schema.add("\"keywords\":[<keywords>]");
	}
	if(extractAuthors) {
	    jsonPrompt+="You should include a list of authors of the text. The authors should be a valid JSON list of strings. ";
	    schema.add("\"authors\":[<authors>]");
	}
	if(extractLocations) {
	    jsonPrompt+="You should also include a list of geographic locations that the text mentions. This list should be a valid JSON list of strings. There should be no more than 4 geographic locations extracted ";
	    schema.add("\"locations\":[<locations>]");
	}
	if(extractLatLon) {
	    jsonPrompt+="If you can, also extract the latitude and longitude of the area that this document describes. Only return the latitude and longitude if you are sure of the location. "; 
	    schema.add("\"latiude\":<latitude>,\"longitude\":<longitude>");
	}	

	jsonPrompt +="\nThe result JSON must adhere to the following schema. It is imperative that nothing is added to the result that does not match. \n{" + Utils.join(schema,",")+"}\n";
	if(debug) System.err.println("Prompt:" + jsonPrompt);

	String json = callLLM(request, jsonPrompt+"\nThe text:\n","",llmCorpus,2000,true,info);
	if(!stringDefined(json)) {
	    String msg = "Failed to extract information for entry:" + entry.getName();
	    getSessionManager().addSessionErrorMessage(request,msg);
	    return false;
	}

	//	    getLogManager().logSpecial("LLMManager:success:" + entry.getName());
	json = json.replaceAll("^```json","").replaceAll("```$","").trim();
	//	    System.err.println("JSON:" + json);
	try {
	    if(!json.startsWith("{")) {
		int idx = json.indexOf("{");
		if(idx>=0) {
		    json = json.substring(idx);
		}
	    }
	    JSONObject obj;
	    try {
		obj = new JSONObject(json);
	    } catch(Exception exc) {
		//Try capping it
		json+="\"\n}";
		obj = new JSONObject(json);
	    }

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
		    summary = "+toggleopen Summary\n+callout-info\n" + sb.toString().trim()+"\n-callout-info\n-toggle\n";
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
	    if(extractLocations) {
		JSONArray array = obj.optJSONArray("locations");
		if(array!=null) {
		    for (int i = 0; i < array.length(); i++) {
			if(i>=10) break;
			getMetadataManager().addMetadata(request, entry,
							 "content.location", MetadataManager.CHECK_UNIQUE_TRUE,
							 array.getString(i));
		    }
		}
	    }
	    if(extractLatLon) {
		double latitude = obj.optDouble("latitude",Double.NaN);
		double longitude = obj.optDouble("longitude",Double.NaN);		
		if(!Double.isNaN(latitude) && !Double.isNaN(longitude)){
		    entry.setLocation(latitude,longitude);
		}
	    }

	    return true;
	} catch(Exception exc) {
	    getSessionManager().addSessionErrorMessage(request,"Error doing LLM extraction:" + entry+" " + exc.getMessage());
	    getLogManager().logSpecial("LLMManager:Error parsing JSON:" + exc+" json:" + json);
	    exc.printStackTrace();
	}
	return false;

    }



    public Result processDocumentChat(Request request, Entry entry,boolean document)
	throws Exception {	
	String pageUrl =request.toString();
	String subLabel = HU.href(pageUrl,document?"Document Chat":"Entry LLM",HU.cssClass("ramadda-clickable"));
        StringBuilder sb      = new StringBuilder();
	String title = entry.getName() +" - " +(document?"Document Chat":"LLM Chat");
        if (request.isAnonymous()) {
	    if(request.exists("question")) {
		return makeJsonError("You must be logged in to use the document chat");
	    } 

	    getPageHandler().entrySectionOpen(request, entry, sb, subLabel);
            sb.append(
		      getPageHandler().showDialogError(
						       "You must be logged in to do document chat"));
	    getPageHandler().entrySectionClose(request, entry, sb);
	    return getEntryManager().addEntryHeader(request, entry,
						    new Result(title, sb));
	}

	if(request.exists("question")) {
	    try {
		PromptInfo info = new PromptInfo(TOKEN_LIMIT_UNDEFINED,request.get("offset",0));
		String r = applyPromptToDocument(request,
						 entry,
						 document,
						 request.getString("question",""),
						 info);
		String s;
		if(r==null) {
		    return makeJsonError("Could not process request");
		} else {
		    s =  JsonUtil.map(Utils.makeListFromValues("offset",info.offset,
						     "corpusLength",info.corpusLength,
						     "segmentLength",info.segmentLength,
						     "tokenCount",info.tokenLimit,
						     "response", JsonUtil.quote(r)));

		}
		return  new Result("", new StringBuilder(s), JsonUtil.MIMETYPE);
	    } catch(Exception exc) {
		Throwable     inner     = LogUtil.getInnerException(exc);
		getLogManager().logError("Error running document chat:" + entry.getName(),exc);
		return makeJsonError("An error has occurred:" + inner);
	    }
	} 

	getPageHandler().entrySectionOpen(request, entry, sb, subLabel);
	sb.append("<table class=ramadda-llm-chat width=100%><tr valign=top><td width=50% style='overflow: hidden;max-width:100%;overflow-x:hidden;'>");
	//hacky
	if(entry.getTypeHandler().isType("type_document_pdf")) {
	    String url = HU.url(getEntryManager().getEntryResourceUrl(request, entry),"fileinline","true");
	    sb.append(HU.getPdfEmbed(url,Utils.makeMap("width","100%")));
	}   else if(entry.getTypeHandler().isType("link")) {
	    String url = entry.getResource().getPath();
	    sb.append("<iframe style='border:var(--basic-border);' src='"+ url+"' width='100%' height='700px' frameborder='1'></iframe>\n");
	} else if(entry.getTypeHandler().isType("type_document_msfile")) {
	    String url = request.getAbsoluteUrl(getEntryManager().getEntryResourceUrl(request, entry));
	    url =HU.url(url,"timestamp",""+entry.getChangeDate());
	    url = url.replace("?","%3F").replace("&","%26");
	    sb.append("<iframe style='border:var(--basic-border);' src='https://view.officeapps.live.com/op/embed.aspx?src="+ url+"' width='100%' height='700px' frameborder='1'></iframe>\n");
	} else {
	    String wiki = "<div style=border:var(--basic-border);'>{{import showTitle=false entry=" + entry.getId()+"}}</div>";
	    sb.append(getWikiManager().wikifyEntry(request, entry, wiki));
	}
	sb.append("</td><td width=50%>");
        String id = HU.getUniqueId("chat_div");
	HU.div(sb,"",HU.attrs("style","width:100%;","id", id));
	sb.append("</td><tr></table>");
	HU.importJS(sb,getPageHandler().makeHtdocsUrl("/wiki.js"));
	HU.importJS(sb,getPageHandler().makeHtdocsUrl("/documentchat.js"));
	List<String> models = new ArrayList<String>();
	for(HtmlUtils.Selector sel:getAvailableModels()) {
	    models.add(JsonUtil.map("value",JsonUtil.quote(sel.getId()),"label",JsonUtil.quote(sel.getLabel())));
	}
	

	HU.script(sb, HU.call("new DocumentChat", HU.squote(id),HU.squote(entry.getId()),
			      JU.quote(document?"documentchat":"entryllm"),
			      JsonUtil.list(models,false)));
        getPageHandler().entrySectionClose(request, entry, sb);
        return getEntryManager().addEntryHeader(request, entry,
						new Result(title, sb));
    }


    private Result makeJsonError(String msg) {
	String s =  JsonUtil.mapAndQuote(Utils.makeListFromValues("error", msg));
	return  new Result("", new StringBuilder(s), JsonUtil.MIMETYPE);
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

    private static class PromptInfo {
	int tokenLimit=TOKEN_LIMIT_UNDEFINED;
	int tokenCount;

	int offset;
	int corpusLength;
	int segmentLength;
	PromptInfo() {
	}

	PromptInfo(int tokenLimit) {
	    this.tokenLimit = tokenLimit;
	}

	PromptInfo(int tokenLimit, int offset) {
	    this(tokenLimit);
	    this.offset = offset;
	}	
    }

    private static class CallException extends RuntimeException {
	public CallException(String msg) {
	    super(msg);
	}
    }

}


