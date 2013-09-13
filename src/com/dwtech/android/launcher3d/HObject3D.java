package com.dwtech.android.launcher3d;

public abstract class HObject3D {
	protected int mId;
	protected String mName;
	
	public void setName(String name) {
		mName = name;
	}

	public String getName() {
		return mName;
	}
}
