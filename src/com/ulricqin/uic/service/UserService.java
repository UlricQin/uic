package com.ulricqin.uic.service;

import java.util.Date;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.ulricqin.uic.model.Session;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.plugin.MemcachePlugin;

public class UserService {

	public static User getByName(String name) {
		Long id = getIdByName(name);
		if (id == null) {
			return null;
		}

		return getById(id);
	}

	public static Long getIdByName(String name) {
		String key = "u:id:" + name;
		Long id = (Long) MemcachePlugin.client.get(key);
		if (id == null) {
			User obj = User.dao.findFirst("select id from user where name = ?",
					name);
			if (obj == null) {
				return null;
			}

			id = obj.getLong("id");
			MemcachePlugin.client.set(key, id);
		}

		return id;
	}

	public static User getById(Object id) {
		String key = "u:" + id;
		User obj = (User) MemcachePlugin.client.get(key);
		if (obj == null) {
			obj = User.dao.findById(id,
					"id,name,cnname,email,phone,im,qq,role,creator,created");
			MemcachePlugin.client.set(key, obj);
		}
		return obj;
	}

	@Before(Tx.class)
	public static boolean deleteUser(User toDelete) {
		Long id = toDelete.getLong("id");
		List<Record> rs = Db.find(
				"select tid from rel_team_user where uid = ?", id);
		for (Record record : rs) {
			MemcachePlugin.client
					.delete("t:" + record.getLong("tid") + ":uids");
		}

		Db.update("delete from rel_team_user where uid = ?", id);
		return toDelete.delete();
	}

	public static User getBySig(String sig) {
		Session s = Session.dao.getBySig(sig);
		if (s == null) {
			return null;
		}

		long expired = s.getLong("expired");
		if (expired < new Date().getTime() / 1000) {
			return null;
		}

		return getById(s.getLong("uid"));
	}

}
