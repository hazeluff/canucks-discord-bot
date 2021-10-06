package com.hazeluff.nhl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import com.hazeluff.nhl.util.JSONOperation;

public class GameJSONUpdate {
	private final JSONOperation op;
	private final List<String> path;
	private final Object value;

	private GameJSONUpdate(JSONOperation op, List<String> path, Object value) {
		this.op = op;
		this.path = path;
		this.value = value;
	}

	public static GameJSONUpdate parse(JSONObject json) {
		JSONOperation op = JSONOperation.parse(json.getString("op"));
		List<String> path = Arrays.asList(json.getString("path").substring(1).split("/"));
		Object value = json.get("value");

		return new GameJSONUpdate(op, path, value);
	}

	public JSONOperation getOp() {
		return op;
	}

	public List<String> getPath() {
		return new ArrayList<>(path);
	}

	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "GameUpdateJSON [op=" + op + ", path=" + path + ", value=" + value + "]";
	}
}
