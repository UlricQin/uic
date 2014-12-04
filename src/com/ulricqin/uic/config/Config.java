package com.ulricqin.uic.config;

import org.apache.commons.lang3.StringUtils;

import com.jfinal.config.Constants;
import com.jfinal.config.Handlers;
import com.jfinal.config.Interceptors;
import com.jfinal.config.JFinalConfig;
import com.jfinal.config.Plugins;
import com.jfinal.config.Routes;
import com.jfinal.core.JFinal;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.c3p0.C3p0Plugin;
import com.ulricqin.frame.kit.LDAP;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.uic.controller.AdminController;
import com.ulricqin.uic.controller.AuthController;
import com.ulricqin.uic.controller.HealthController;
import com.ulricqin.uic.controller.MainController;
import com.ulricqin.uic.controller.SenderController;
import com.ulricqin.uic.controller.SsoController;
import com.ulricqin.uic.controller.TeamController;
import com.ulricqin.uic.controller.UserController;
import com.ulricqin.uic.interceptor.GlobalInterceptor;
import com.ulricqin.uic.model.Session;
import com.ulricqin.uic.model.Team;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.plugin.MemcachePlugin;

public class Config extends JFinalConfig {
	
	public static String[] smsUrls;
	public static String[] mailUrls;
	public static String maintainer = "";
	public static boolean canRegister;
	public static String token;
	public static boolean ldapEnabled;

	@Override
	public void configConstant(Constants me) {
		loadPropertyFile("config.txt");
		me.setDevMode(getPropertyToBoolean("devMode", false));
		me.setBaseViewPath("/WEB-INF/tpl");
		
		String sms = getProperty("smsSender");
		if (StringKit.isNotBlank(sms)) {
			smsUrls = StringUtils.split(sms, ",;");
		}
		
		String mail = getProperty("mailSender");
		if (StringKit.isNotBlank(mail)) {
			mailUrls = StringUtils.split(mail, ",;");
		}
		
		maintainer = getProperty("maintainer");
		canRegister = getPropertyToBoolean("canRegister", false);
		token = getProperty("token");
		
		ldapEnabled = getPropertyToBoolean("ldapEnabled", false);
		if (ldapEnabled) {
			LDAP.initGlobalConfig(getProperty("ldapHost"), getPropertyToInt("ldapPort"), getProperty("ldapBase"), getProperty("ldapUid"), getProperty("ldapBindDn"), getProperty("ldapPassword"));
		}
	}

	@Override
	public void configRoute(Routes me) {
		me.add("/", MainController.class);
		me.add("/health", HealthController.class);
		me.add("/auth", AuthController.class);
		me.add("/admin", AdminController.class);
		me.add("/user", UserController.class);
		me.add("/team", TeamController.class);
		me.add("/sso", SsoController.class);
		me.add("/sender", SenderController.class);
	}

	@Override
	public void configPlugin(Plugins me) {
		String jdbcUrl = getProperty("jdbcUrl").trim();
		String jdbcUser = getProperty("user").trim();
		String jdbcPasswd = getProperty("password").trim();

		C3p0Plugin c3p0Plugin = new C3p0Plugin(jdbcUrl, jdbcUser, jdbcPasswd);
		me.add(c3p0Plugin);

		ActiveRecordPlugin arp = new ActiveRecordPlugin(c3p0Plugin);
		arp.setShowSql(getPropertyToBoolean("devMode", false));
		me.add(arp);

		arp.addMapping("team", Team.class);
		arp.addMapping("user", User.class);
		arp.addMapping("session", Session.class);
		
		String memcacheAddrs = getProperty("memcacheAddrs").trim();
		String[] addrs = StringUtils.split(memcacheAddrs, ',');
		Integer[] weights = new Integer[addrs.length];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = new Integer(10);
		}
		MemcachePlugin memcachePlugin = new MemcachePlugin(addrs, weights);
		me.add(memcachePlugin);
	}

	@Override
	public void configInterceptor(Interceptors me) {
		me.add(new GlobalInterceptor());
	}

	@Override
	public void configHandler(Handlers me) {
	}

	public static void main(String[] args) {
		JFinal.start("web", 8077, "/", 5);
	}

}
