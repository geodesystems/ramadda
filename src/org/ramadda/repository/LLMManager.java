/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.repository;
import org.ramadda.repository.admin.*;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.JsonUtil;

import ucar.unidata.util.Misc;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

import org.json.*;

/**
 * Handles LLM requests
 *
 */
@SuppressWarnings("unchecked")
public class LLMManager extends  AdminHandlerImpl {

    private static boolean  debugLLM = false;
    public static final int GPT3_TOKEN_LIMIT = 2000;
    public static final int GPT4_TOKEN_LIMIT = 4000;



    public LLMManager(Repository repository) {
        super(repository);
    }

    public String getId() {
        return "llmmanager";
    }

    public boolean isLLMEnabled() {
	return Utils.stringDefined(getRepository().getProperty("gpt.api.key")) ||
	    Utils.stringDefined(getRepository().getProperty("palm.api.key"));
    }

    public boolean isGPT4Enabled() {
	return getRepository().getProperty("gpt.gpt4.enabled",false);
    }    




    public Result processLLM(Request request)  throws Exception {
	if(request.isAnonymous()) {
	    String json = JsonUtil.map(Utils.makeList("error", JsonUtil.quote("You must be logged in to use the rewrite service")));
	    return new Result("", new StringBuilder(json), "text/json");
	}

	String text =request.getString("text","");
	String promptPrefix = request.getString("promptprefix",
						"Rewrite the following text as college level material:");
	String promptSuffix = request.getString("promptsuffix", "");
	//	text = callGpt("Rewrite the following text:","",new StringBuilder(text),1000,false);		    
	text = callLLM(request, promptPrefix,promptSuffix,new StringBuilder(text),1000,false,null,0);		    
	String json = JsonUtil.map(Utils.makeList("result", JsonUtil.quote(text)));
	return new Result("", new StringBuilder(json), "text/json");
	
    }

    private Result makeJsonErrorResult(String error) {
	String json = JsonUtil.map(Utils.makeList("error", JsonUtil.quote(error)));
	return new Result("", new StringBuilder(json), "text/json");
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
	String gptKey = getRepository().getProperty("gpt.api.key");
	String palmKey = getRepository().getProperty("palm.api.key");
	if(gptKey==null && palmKey==null) {
	    if(debugLLM) System.err.println("\tno LLM keys");
	    return makeJsonErrorResult("LLM processing is not enabled");
	}

	File file = new File(request.getUploadedFile("audio-file"));
	String[]args = new String[]{"Authorization","Bearer " +gptKey};
	System.err.println("key:" + args[0]+":");
	String mime = request.getString("mimetype","audio/webm");

	String fileName = "audio" + mime.replaceAll(".*/",".");
	//	fileName = "test.mp4";
	//	file = new File("/Users/jeffmc/test.mp4");
	//	mime = "audio/mp4";

	List postArgs   =Utils.add(new ArrayList(),
				   "model","whisper-1","file", new IO.FileWrapper(file,fileName,mime));
	System.err.println(postArgs);
	URL url =  new URL("https://api.openai.com/v1/audio/transcriptions");
	IO.Result result =  IO.doMultipartPost(url, args,postArgs);
	System.err.println("transcribe:" + result);
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


    public String callLLM(Request request, String prompt1,String prompt2,StringBuilder corpus,int maxReturnTokens,boolean tokenize,int[]initTokenLimit,int callCnt)
	throws Exception {
	String text = corpus.toString();
	String gptKey = getRepository().getProperty("gpt.api.key");
	String palmKey = getRepository().getProperty("palm.api.key");	
	if(gptKey==null && palmKey==null) {
	    if(debugLLM) System.err.println("\tno LLM key");
	    return null;
	}

	boolean useGPT4 = false;
	if(isGPT4Enabled()) {
	    useGPT4 = request.get(ARG_USEGPT4,false);
	}

	if(initTokenLimit==null) 
	    initTokenLimit = new int[]{-1};

	if(initTokenLimit[0]<=0) {
	    if(useGPT4)
		initTokenLimit[0] = GPT4_TOKEN_LIMIT;
	    else
		initTokenLimit[0] = GPT3_TOKEN_LIMIT;
	} 


	debugLLM = true;
	int tokenLimit = initTokenLimit[0];
	while(tokenLimit>=500) {
	    initTokenLimit[0] = tokenLimit;
	    StringBuilder gptCorpus = new StringBuilder(prompt1);
	    gptCorpus.append("\n\n");
	    if(!tokenize) {
		gptCorpus.append(text);
	    } else {
		if(debugLLM) System.err.println("Repository.callGpt tokenizing " + text.length());
		text = Utils.removeNonAscii(text," ").replaceAll("[,-\\.\n]+"," ").replaceAll("  +"," ");
		if(text.trim().length()==0) {
		    if(debugLLM) System.err.println("\tRepository.callGpt no text");
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
		if(debugLLM) System.err.println("\tRepository.callGpt done tokenizing: i=" + i +" extraCnt=" + extraCnt);
	    }
	    gptCorpus.append("\n\n");
	    gptCorpus.append(prompt2);
	    String gptText =  gptCorpus.toString();
	    if(debugLLM) System.err.println("Repository.callGpt  max return tokens:" + maxReturnTokens +" use gpt4:" + useGPT4 +" text length:" + gptText.length() +" tokenCnt:" + initTokenLimit[0]);
	    List<String> args = Utils.makeList("temperature", "0",
					       "max_tokens" ,""+ maxReturnTokens,
					       "top_p", "1.0");

	    if(useGPT4) 
		Utils.add(args,"model",JsonUtil.quote("gpt-4"));
	    else
		Utils.add(args,"model",JsonUtil.quote("gpt-3.5-turbo"));
	    Utils.add(args,"messages",JsonUtil.list(JsonUtil.map(
								 "role",JsonUtil.quote("user"),
								 "content",JsonUtil.quote(gptText))));

	    String body = JsonUtil.map(args);
	    String url = "https://api.openai.com/v1/chat/completions";
	    IO.Result  result = IO.getHttpResult(IO.HTTP_METHOD_POST
						 , new URL(url), body,
						 "Content-Type","application/json",
						 "Authorization","Bearer " +gptKey);
	    if(result.getError()) {
		try {
		    JSONObject json = new JSONObject(result.getResult());
		    JSONObject error = json.optJSONObject("error");
		    if(error == null) {
			System.err.println("Error calling OpenAI. No error in result:" + result.getResult());
			return null;
		    }

		    String code = error.optString("code",null);
		    if(code == null) {
			System.err.println("Error calling OpenAI. No code in result:" + result.getResult());
			return null;
		    }

		    if(code.equals("rate_limit_exceeded")) {
			System.err.println("Calling OpenAI: rate limit exceeded");
			System.err.println(result.getResult());
			callCnt++;
			if(callCnt>3) return null;
			Misc.sleepSeconds(callCnt*60);
			tokenLimit-=1000;
			continue;
		    }

		    if(!code.equals("context_length_exceeded")) {
			System.err.println("Error calling OpenAI. Unknown error code:" + result.getResult());
			return null;
		    }
		    tokenLimit-=1000;
		    if(debugLLM)
			System.err.println("Repostory.callGpt: too many tokens. Trying again with limit:" + tokenLimit);
		    continue;
		} catch(Exception exc) {
		    exc.printStackTrace();
		    throw new RuntimeException("Unable to process GPT request:" + result.getResult());
		}
	    }

	    JSONObject json = new JSONObject(result.getResult());
	    if(debugLLM)	    System.err.println("LLM json:" + json);		
	    //Google PALM
	    if(json.has("candidates")) {
		JSONArray candidates = json.getJSONArray("candidates");
		JSONObject candidate= candidates.getJSONObject(0);
		return candidate.optString("output",null);
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
		    System.err.println("No results from GPT:" + result);
		}
	    }
	}
	return null;
    }


}


