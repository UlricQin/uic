package com.ulricqin.uic.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.uic.plugin.MemcachePlugin;
import com.ulricqin.uic.service.UserService;

public class Team extends Model<Team> {

	private static final long serialVersionUID = 64630477990740316L;
	public static final Team dao = new Team();

	// 我创建的组、我参与的组
	public Page<Team> paginate(int pageNo, int pageSize, String query,
			boolean iAmCreator, User creator) {

		Long id = creator.getLong("id");
		StringBuilder where = new StringBuilder("from team where 1=1");
		List<Object> params = new LinkedList<Object>();

		if (iAmCreator) {
			List<Long> teamIds = new LinkedList<Long>();
			List<Record> rs = Db.find(
					"select tid from rel_team_user where uid = ?", id);
			for (Record record : rs) {
				teamIds.add(record.getLong("tid"));
			}

			String orClause = "";
			if (teamIds.size() > 0) {
				orClause = "or id in (" + StringUtils.join(teamIds, ',') + ")";
			}
			where.append(" and (creator = ? " + orClause + ")");
			params.add(id);
		}

		if (StringKit.isNotBlank(query)) {
			String tmp = "%" + query + "%";
			where.append(" and name like ?");
			params.add(tmp);
		}

		where.append(" order by name");

		Page<Team> ret = paginate(pageNo, pageSize, "select *",
				where.toString(), params.toArray());

		return ret;
	}

	public List<User> getUsers() {
		List<User> users = new LinkedList<User>();
		List<Long> ids = userIds();
		if (ids == null || ids.size() == 0) {
			return users;
		}

		for (Long id : ids) {
			users.add(UserService.getById(id));
		}

		return users;
	}

	@SuppressWarnings("unchecked")
	public List<Long> userIds() {
		Long tid = getLong("id");
		String key = String.format("t:%s:uids", tid.toString());

		Object val = MemcachePlugin.client.get(key);
		if (val == null) {
			List<Record> rs = Db.find(
					"select uid from rel_team_user where tid = ?", tid);
			if (rs == null || rs.size() == 0) {
				return Collections.emptyList();
			}

			List<Long> userIds = new LinkedList<Long>();
			for (Record record : rs) {
				userIds.add(record.getLong("uid"));
			}
			MemcachePlugin.client.set(key, userIds);
			return userIds;
		}

		return (List<Long>) val;
	}

	public String userIdsStr() {
		List<Long> userIds = userIds();
		if (userIds.size() == 0) {
			return "";
		}
		return StringUtils.join(userIds, ',');
	}

	public void addUsers(String userIds) {
		if (StringKit.isNotBlank(userIds)) {
			Long tid = this.getLong("id");
			String[] ids = StringUtils.split(userIds, ',');
			for (int i = 0; i < ids.length; i++) {
				Db.update("insert into rel_team_user(tid,uid) values(?, ?)",
						tid, Integer.parseInt(ids[i]));
			}
		}
	}

	@Before(Tx.class)
	public void up(String resume, String userIds) {
		this.set("resume", resume);
		this.update();

		Long id = this.getLong("id");
		MemcachePlugin.client.delete("t:" + id);
		Db.update("delete from rel_team_user where tid = ?", id);
		MemcachePlugin.client.delete("t:" + id + ":uids");
		this.addUsers(userIds);
	}

	@Override
	public boolean delete() {
		boolean ret = this.deleteTeamFromDB();
		if (ret) {
			Long id = this.getLong("id");
			// cache: object
			MemcachePlugin.client.delete("t:" + id);
			// cache: name -> id
			MemcachePlugin.client.delete("t:id:" + this.getStr("name"));
			// cache: user ids of team
			MemcachePlugin.client.delete("t:" + id + ":uids");
		}
		return ret;
	}

	@Before(Tx.class)
	public boolean deleteTeamFromDB() {
		boolean ret = super.delete();
		if (ret) {
			Db.update("delete from rel_team_user where tid = ?",
					this.getLong("id"));
		}
		return ret;
	}
	
	public List<Team> query(String query, int limit) {
		if (StringKit.isBlank(query)) {
			return find("select id,name,resume,creator from team limit ?", limit);
		}

		return find(
				"select id,name,resume,creator from team where name like ? limit ?",
				"%" + query + "%", limit);
	}

}
