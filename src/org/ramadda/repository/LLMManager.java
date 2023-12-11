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
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.net.URL;

import org.json.*;

/**
 * Handles LLM requests
 *
 */
@SuppressWarnings("unchecked")
public class LLMManager extends  AdminHandlerImpl {

    public static boolean  debug = false;
    public static final int GPT3_TOKEN_LIMIT = 2000;
    public static final int GPT4_TOKEN_LIMIT = 4000;

    public static final String GPT_MODEL_3_5="gpt-3.5-turbo-1106";
    public static final String GPT_MODEL_4="gpt-4";

    public LLMManager(Repository repository) {
        super(repository);
    }

    public void debug(String msg) {
	if(debug) System.err.println("LLMManager:" + msg);
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
	text = callLLM(request, promptPrefix,promptSuffix,text,1000,false,null,0);		    
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

    private List<String> gptKeys;
    private int gptKeyIdx=0;
    private synchronized String getGptKey() {
	if(gptKeys==null) {
	    String gptKey = getRepository().getProperty("gpt.api.key");
	    if(gptKey!=null) gptKeys = Utils.split(gptKey,",",true,true);
	    else gptKeys = new ArrayList<String>();
	}
	if(gptKeys.size()==0) {
	    return null;
	}	    
	gptKeyIdx++;
	if(gptKeyIdx>=gptKeys.size()) gptKeyIdx=0;
	return gptKeys.get(gptKeyIdx);
    }

    private Result processTranscribeInner(Request request)  throws Exception {	
	if(request.isAnonymous()) {
	    return makeJsonErrorResult("You must be logged in to use the rewrite service");
	}

	String palmKey = getRepository().getProperty("palm.api.key");
	String gptKey = getGptKey();
	if(gptKey==null && palmKey==null) {
	    if(debug) System.err.println("\tno LLM keys");
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


    public synchronized String callLLM(Request request,
				       String prompt1,
				       String prompt2,
				       String corpus,
				       int maxReturnTokens,
				       boolean tokenize,
				       int[]initTokenLimit,
				       int callCnt)
	throws Exception {
	String text = corpus.toString();
	String gptKey = getGptKey();
	String palmKey = getRepository().getProperty("palm.api.key");	
	if(gptKey==null && palmKey==null) {
	    if(debug) System.err.println("\tno LLM key");
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


	debug = true;
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
		    if(debug) System.err.println("LLMManager.callGpt no text");
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
		if(debug) System.err.println("\ttokenizing init length=" + text.length()+
					     " max return tokens:" + maxReturnTokens+
					     " #toks=" + i +
					     " extra cnt=" + extraCnt);

	    }
	    gptCorpus.append("\n\n");
	    gptCorpus.append(prompt2);
	    String gptText =  gptCorpus.toString();
	    if(debug) System.err.println("\tuse gpt4:" + useGPT4 +
					 " text length:" + gptText.length() +
					 " token limit:" + initTokenLimit[0]);

	    List<String> args = Utils.makeList("temperature", "0",
					       "max_tokens" ,""+ maxReturnTokens,
					       "top_p", "1.0");

	    if(useGPT4) 
		Utils.add(args,"model",JsonUtil.quote(GPT_MODEL_4));
	    else
		Utils.add(args,"model",JsonUtil.quote(GPT_MODEL_3_5));
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
			System.err.println("\tLLMManager Error: No code in result:" + result.getResult());
			return null;
		    }

		    if(code.equals("rate_limit_exceeded")) {
			//https://platform.openai.com/docs/guides/rate-limits/usage-tiers?context=tier-two
			long ms= 1000*callCnt*60;
			String delay = StringUtil.findPattern(result.getResult(),"Please try again in ([\\d\\.]+)s");
			if(delay!=null) {
			    try {ms = (int)(1000*1.5*Double.parseDouble(delay));} catch(Exception ignore){}
			    //			    System.err.println("\tseconds delay:" + delay +" ms:" + ms);
			} else {
			    delay = StringUtil.findPattern(result.getResult(),"Please try again in ([\\d\\.]+)ms");
			    if(delay!=null) {
				try {ms = (int)(1.5*Double.parseDouble(delay));} catch(Exception ignore){}
				//				System.err.println("\tms delay:" + delay +" ms:" + ms);
			    }
			}
			
			callCnt++;
			if(callCnt>3) return null;
			System.err.println("\tLLMManager: rate limit exceeded."+
					   " sleeping for:" +  ms+" ms"  +
					   " message:" +error.optString("message",""));

			Misc.sleep(ms);
			tokenLimit-=1000;
			continue;
		    }

		    if(!code.equals("context_length_exceeded")) {
			System.err.println("\tLLMManager: Error calling OpenAI. Unknown error code:" + result.getResult());
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
		    System.err.println("\tresult: prompt tokens:" + tokens+
				       " completion tokens:" + JsonUtil.readValue(json,"usage.completion_tokens","NA"));
		} else {
		    System.err.println("\tLLMManager: json: "  +Utils.clip(result.getResult(),200,"..."));
		}
	    }
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
		    System.err.println("\tLLMManager:No results from GPT:" + result);
		}
	    }
	}
	return null;
    }

    public String extractTitle(Request request, String corpus,int[]tokenLimit) throws Exception {
	if(debug) System.err.println("LLMManager: extractTitle");
	String title = callLLM(request, "Extract the title from the following document:","",
					       corpus,200,true,tokenLimit,0);
	//	if(debug) System.err.println("\ttitle=" + title);
	return title;
    }

    public List<String>  getKeywords(Request request, Entry entry, String llmCorpus, int[]tokenLimit)
	throws Exception {
	String path = entry.getResource().getPath();
	if(path==null) return null;
	if(!getSearchManager().isDocument(path)) {
	    //	    System.err.println("not doc:" + path);
	    return null;
	}

	List<String> keywords = new ArrayList<String>();
	if(LLMManager.debug) System.err.println("LLMManager.getKeywords");
	String result = callLLM(request,
				"Extract keywords from the following text. Limit your response to no more than 10 keywords:",
				"Keywords:",
				llmCorpus,
				60,true,tokenLimit,0);
	if(result!=null) {
	    for(String tok:Utils.split(result,",",true,true)) {
		if(keywords.size()>15) break;
		if(!keywords.contains(tok)) {
		    keywords.add(tok);
		}
	    }
	}
							  
	if(keywords.size()==0) {
	    llmCorpus = Utils.removeNonAscii(llmCorpus," ").replaceAll("[,-\\.\n]+"," ").replaceAll("  +"," ");
	    Hashtable<String,WordCount> cnt = new Hashtable<String,WordCount>();
	    List<WordCount> words = new ArrayList<WordCount>();
	    HashSet stopWords = Utils.getStopWords();
	    for(String tok: Utils.split(llmCorpus," ",true,true)) {
		tok = tok.toLowerCase();
		if(tok.length()<=2) continue;
		if(stopWords.contains(tok)) continue;
		//		System.out.println("TOK:" + tok);
		WordCount word = cnt.get(tok);
		if(word==null) {
		    word = new WordCount(tok);
		    words.add(word);
		    cnt.put(tok,word);
		}
		word.count++;
	    }
	    Collections.sort(words, new Comparator() {
		    public int compare(Object o1, Object o2) {
			WordCount w1 = (WordCount) o1;
			WordCount w2 = (WordCount) o2;			
			if(w2.count==w1.count) {
			    return w2.word.length()-w1.word.length();
			}
			return w2.count - w1.count;
		    }
		});

	    for(int i=0;i<words.size()&& i<3;i++) {
		WordCount word = words.get(i);
		if(word.count>2) {
		    keywords.add(word.word);
		}
	    }
	}
	return keywords;
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


