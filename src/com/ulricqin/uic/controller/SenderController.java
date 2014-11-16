package com.ulricqin.uic.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.uic.config.Config;
import com.ulricqin.uic.interceptor.LoginRequiredInterceptor;
import com.ulricqin.uic.model.Team;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.service.TeamService;
import com.ulricqin.uic.service.UserService;

public class SenderController extends Controller {

	@Before(LoginRequiredInterceptor.class)
	public void index() {
		render("../sender.html");
	}

	public void mail() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		String content = getPara("content", "");
		if (content.equals("")) {
			renderText("content is blank");
			return;
		}

		String subject = getPara("subject", "");
		if (subject.equals("")) {
			subject = content;
		}

		String device = "email";
		List<String> tos = _tos(device);
		if (tos == null || tos.size() == 0) {
			return;
		}

		String msg = _mail(tos, subject, content);
		if (msg.equals("")) {
			msg = "ok";
		}
		renderText(msg);
	}

	public void sms() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		String content = getPara("content", "");
		if (content.equals("")) {
			renderText("content is blank");
			return;
		}

		String device = "phone";
		List<String> tos = _tos(device);
		if (tos == null || tos.size() == 0) {
			return;
		}

		String msg = _sms(tos, content);
		if (msg.equals("")) {
			msg = "ok";
		}
		renderText(msg);
	}

	private String _sms(List<String> tos, String content) {
		String[] urls = Config.smsUrls;
		if (urls == null || urls.length == 0) {
			return "";
		}

		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			for (int i = 0; i < urls.length; i++) {
				HttpPost httpPost = new HttpPost(urls[i]);

				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("tos", StringUtils.join(tos,
						';')));
				nvps.add(new BasicNameValuePair("content", content));
				httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

				CloseableHttpResponse resp = httpClient.execute(httpPost);
				try {
					StatusLine statusLine = resp.getStatusLine();
					HttpEntity entity = resp.getEntity();
					String respContent = EntityUtils.toString(entity);
					if (statusLine.getStatusCode() == 200
							&& (respContent.equals("") || respContent
									.equals("ok"))) {
						break;
					} else {
						// fail
						if (i + 1 == urls.length) {
							return respContent;
						} else {
							// send alarm to maintainer
							String nextSmsServer = urls[i + 1];
							httpPost = new HttpPost(nextSmsServer);
							nvps = new ArrayList<NameValuePair>();
							nvps.add(new BasicNameValuePair("tos",
									Config.maintainer));
							nvps.add(new BasicNameValuePair("content", urls[i]
									+ " down"));
							httpPost.setEntity(new UrlEncodedFormEntity(nvps,
									"UTF-8"));
							httpClient.execute(httpPost);
						}
					}
				} finally {
					resp.close();
				}
			}
			return "";
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		} catch (ClientProtocolException e) {
			return e.getMessage();
		} catch (IOException e) {
			return e.getMessage();
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				return e.getMessage();
			}
		}
	}

	private String _mail(List<String> tos, String subject, String content) {
		String[] urls = Config.mailUrls;
		if (urls == null || urls.length == 0) {
			return "";
		}

		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			for (int i = 0; i < urls.length; i++) {
				HttpPost httpPost = new HttpPost(urls[i]);

				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("tos", StringUtils.join(tos,
						';')));
				nvps.add(new BasicNameValuePair("subject", subject));
				nvps.add(new BasicNameValuePair("content", content));
				httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

				CloseableHttpResponse resp = httpClient.execute(httpPost);
				try {
					StatusLine statusLine = resp.getStatusLine();
					HttpEntity entity = resp.getEntity();
					String respContent = EntityUtils.toString(entity);
					if (statusLine.getStatusCode() == 200
							&& (respContent.equals("ok") || respContent
									.equals(""))) {
						break;
					} else {
						// fail
						if (i + 1 == urls.length) {
							return respContent;
						} else {
							// send alarm to maintainer
							String nextSmsServer = urls[i + 1];
							httpPost = new HttpPost(nextSmsServer);
							nvps = new ArrayList<NameValuePair>();
							nvps.add(new BasicNameValuePair("tos",
									Config.maintainer));
							nvps.add(new BasicNameValuePair("subject", urls[i]
									+ " down"));
							nvps.add(new BasicNameValuePair("content", urls[i]
									+ " down"));
							httpPost.setEntity(new UrlEncodedFormEntity(nvps,
									"UTF-8"));
							httpClient.execute(httpPost);
						}
					}
				} finally {
					resp.close();
				}
			}
			return "";
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		} catch (ClientProtocolException e) {
			return e.getMessage();
		} catch (IOException e) {
			return e.getMessage();
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				return e.getMessage();
			}
		}
	}

	private List<String> _tos(String device) {
		String userNames = getPara("user", "");
		if (!userNames.equals("")) {
			// 给多个人发
			Set<String> deviceSet = new HashSet<String>();
			String[] arr = StringUtils.split(userNames, ",;");
			for (String a : arr) {
				User u = UserService.getByName(a);
				if (u != null) {
					String ad = u.getStr(device);
					if (StringKit.isNotBlank(ad)) {
						deviceSet.add(ad);
					}
				}
			}

			if (deviceSet.size() == 0) {
				renderText("ok");
				return Collections.emptyList();
			}
			
			List<String> ret = new LinkedList<String>();
			for (String a : deviceSet) {
				ret.add(a);
			}
			return ret;
		}

		// 给多个组发
		String teamNames = getPara("team", "");
		if (StringKit.isBlank(teamNames)) {
			getResponse().setStatus(400);
			renderText("parameter team is blank");
			return Collections.emptyList();
		}

		String[] nameArr = StringUtils.split(teamNames, ",;");
		Set<String> tos = new HashSet<String>();

		for (String teamName : nameArr) {
			Team t = TeamService.getByName(teamName);
			if (t == null) {
				continue;
			}

			List<User> users = t.getUsers();
			if (users == null || users.size() == 0) {
				continue;
			}

			for (User u : users) {
				String aDevice = u.getStr(device);
				if (StringKit.isNotBlank(aDevice)) {
					tos.add(aDevice);
				}
			}
		}

		if (tos.size() == 0) {
			renderText("ok");
			return Collections.emptyList();
		}

		List<String> ret = new LinkedList<String>();
		for (String a : tos) {
			ret.add(a);
		}

		return ret;
	}
}
