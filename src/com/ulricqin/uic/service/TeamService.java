package com.ulricqin.uic.service;

import com.ulricqin.uic.model.Team;
import com.ulricqin.uic.plugin.MemcachePlugin;

public class TeamService {

	public static Team getByName(String name) {
		Long id = getIdByName(name);
		if (id == null) {
			return null;
		}

		return getById(id);
	}

	public static Long getIdByName(String name) {
		String key = "t:id:" + name;
		Long id = (Long) MemcachePlugin.client.get(key);
		if (id == null) {
			Team t = Team.dao.findFirst("select id from team where name = ?",
					name);
			if (t == null) {
				return null;
			}

			id = t.getLong("id");
			MemcachePlugin.client.set(key, id);
		}

		return id;
	}

	public static Team getById(Object id) {
		String key = "t:" + id;
		Team t = (Team) MemcachePlugin.client.get(key);
		if (t == null) {
			t = Team.dao.findById(id);
			MemcachePlugin.client.set(key, t);
		}
		return t;
	}

}
