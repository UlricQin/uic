package com.ulricqin.frame.kit;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;

public class LDAP {

	private static String host;
	private static int port;
	private static String base;
	private static String uid = "sAMAccountName";
	private static String dn;
	private static String password;

	public static void initGlobalConfig(String aHost, int aPort, String aBase,
			String aUid, String aDn, String aPassword) {
		host = aHost;
		port = aPort;
		base = aBase;
		uid = aUid;
		dn = aDn;
		password = aPassword;
	}

	public boolean auth(String userName, String userPassword)
			throws LDAPException {
		LDAPConnection connection = new LDAPConnection(host, port, dn, password);
		SearchResult searchResult = connection.search(base, SearchScope.SUB,
				String.format("(%s=%s)", uid, userName));
		if (searchResult.getEntryCount() == 0) {
			return false;
		}

		String dn = searchResult.getSearchEntries().get(0).getDN();
		try {
			new LDAPConnection(host, port, dn, userPassword);
			return true;
		} catch (LDAPException e) {
			return false;
		}
	}
}