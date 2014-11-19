package com.ulricqin.uic.controller;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.jfinal.aop.Before;
import com.jfinal.aop.ClearInterceptor;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Page;
import com.ulricqin.frame.exception.RenderJsonMsgException;
import com.ulricqin.frame.kit.Checker;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.uic.config.Config;
import com.ulricqin.uic.interceptor.LoginRequiredInterceptor;
import com.ulricqin.uic.interceptor.MakeSureTargetTeamExistsInterceptor;
import com.ulricqin.uic.model.Team;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.service.TeamService;
import com.ulricqin.uic.service.UserService;

@Before(LoginRequiredInterceptor.class)
public class TeamController extends Controller {

	public void index() {
		int teamId = getParaToInt();
		if (teamId < 1) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		Team team = TeamService.getById(teamId);
		if (team == null) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		setAttr("msg", "");
		setAttr("team", team);
		renderJson();
	}
	
	public void users2() {
		int id = getParaToInt(0, 0);
		if (id == 0) {
			throw new RenderJsonMsgException("id is invalid");
		}
		
		Team t = TeamService.getById(id);
		if (t == null) {
			throw new RenderJsonMsgException("no such team");
		}
		
		renderJson("users", t.getUsers());
	}
	
	@ClearInterceptor
	public void users() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		String teamName = getPara("name", "");
		if (teamName.equals("")) {
			throw new RenderJsonMsgException("no such team");
		}
		
		Team t = TeamService.getByName(teamName);
		if (t == null) {
			throw new RenderJsonMsgException("no such team");
		}
		
		renderJson("users", t.getUsers());
	}
	
	public void all() {
		setAttr("title", "Team - UIC");
		setAttr("isTeamPage", true);
		
		String query = getPara("q", "");
		int pageNo = getParaToInt(0, 1);
		User me = getAttr("me");

		setAttr("query", query);
		setAttr("page",
				Team.dao.paginate(pageNo, 6, query, true, me));
	}
	
	@ClearInterceptor
	public void mine() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		int uid = getParaToInt();
		if (uid < 1) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		User u = UserService.getById(uid);
		if (u == null) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		Page<Team> page = Team.dao.paginate(1, 1000, null, true, u);
		if (page == null) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		setAttr("msg", "");
		setAttr("teams", page.getList());
		renderJson();
	}
	
	public void create() {
		String method = getRequest().getMethod();
		if (method.equalsIgnoreCase("GET")) {
			createGet();
		} else {
			createPost();
		}
	}

	private void createGet() {
		setAttr("title", "Create Team - UIC");
		setAttr("isTeamPage", true);
	}

	private void createPost() {
		String name = getPara("name", "");
		name = Jsoup.clean(name, Whitelist.none());
		if (StringKit.isBlank(name)) {
			throw new RenderJsonMsgException("name is blank");
		}
		
		if (!Checker.isIdentifier(name)) {
			throw new RenderJsonMsgException("name is invalid. use a-zA-Z0-9-_");
		}

		Team team = TeamService.getByName(name);
		if (team != null) {
			throw new RenderJsonMsgException("name is already existent");
		}

		String resume = getPara("resume", "");
		resume = Jsoup.clean(resume, Whitelist.none());
		User me = getAttr("me");
		
		Team t = new Team();
		t.set("name", name);
		t.set("resume", resume);
		t.set("creator", me.getLong("id"));
		if (!t.save()) {
			throw new RenderJsonMsgException("occur unknown error");
		}
		
		String userIds = getPara("users", "");
		userIds = Jsoup.clean(userIds, Whitelist.none());
		t.addUsers(userIds);

		renderJson("msg", "");
	}
	
	@Before(MakeSureTargetTeamExistsInterceptor.class)
	public void delete() {
		Team toDelete = getAttr("targetTeam");

		User me = getAttr("me");
		if (!me.canWrite(toDelete)) {
			renderJson("msg", "no privilege");
			return;
		}

		if (toDelete.delete()) {
			renderJson("msg", "");
		} else {
			renderJson("msg", "occur unknown error");
		}
	}
	
	@Before(MakeSureTargetTeamExistsInterceptor.class)
	public void edit() {
		String method = getRequest().getMethod();
		if (method.equalsIgnoreCase("GET")) {
			editGet();
		} else {
			editPost();
		}
	}

	private void editGet() {
		Team toEdit = getAttr("targetTeam");
		setAttr("team", toEdit);
		setAttr("title", "Edit Team - UIC");
		setAttr("isTeamPage", true);
	}

	private void editPost() {
		Team toEdit = getAttr("targetTeam");
		String resume = getPara("resume", "");
		resume = Jsoup.clean(resume, Whitelist.none());
		String userIds = getPara("users", "");
		userIds = Jsoup.clean(userIds, Whitelist.none());
		
		toEdit.up(resume, userIds);
		renderJson("msg", "");
	}
	
	@ClearInterceptor
	public void query() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		String query = getPara("query");
		int limit = getParaToInt("limit", 10);
		setAttr("teams", Team.dao.query(query, limit));
		setAttr("msg", "");
		renderJson();
	}
}
