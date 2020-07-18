package com.amazon.awsworkbench.data;

public class ComponentAttribute {

	private String type;

	private String value;

	private String name;

	public ComponentAttribute(String type, String value, String name) {
		super();
		this.type = type;
		this.value = value;
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
