package com.orbitz.savath;

public class DepEntry {

	private String name;
	private String group;
	private String project;
	private String version;

	public DepEntry(String name, String group, String project, String version) {
		this.name = name;
		this.group = group;
		this.project = project;
		this.version = version;
	}

	@Override
	public boolean equals(Object obj) {
		DepEntry other = (DepEntry) obj;
		return other.name.equals(this.name) && other.group.equals(this.group) && other.project.equals(this.project)
				&& other.version.equals(this.version);
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return name.hashCode() + 3 * version.hashCode() + 5 * project.hashCode() + 7 * group.hashCode() + 11
				* version.hashCode();
	}

	public static void main(String[] args) {
	}
	
	public String getNode() {
		return group + "/" + project + "/" + name + "-" + version + ".jar";
	}
}
