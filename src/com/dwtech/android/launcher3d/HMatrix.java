package com.dwtech.android.launcher3d;

public class HMatrix {
	
	private float[] mData = new float[16];
	
	public HMatrix(boolean i){
		if (i){
			HMatrix.makeIdentity(mData);
		}
	}
	
	public static void makeIdentity(float m[]){
		m[0] = 1;m[4] = 0;m[8] = 0;m[12] = 0;
		m[1] = 0;m[5] = 1;m[9] = 0;m[13] = 0;
		m[2] = 0;m[6] = 0;m[10] = 1;m[14] = 0;
		m[3] = 0;m[7] = 0;m[11] = 0;m[15] = 1;
	}
	
	public static float Determinant4f(float m[]) {
		return m[12] * m[9] * m[6] * m[3] - m[8] * m[13] * m[6] * m[3] - m[12]
				* m[5] * m[10] * m[3] + m[4] * m[13] * m[10] * m[3] + m[8]
				* m[5] * m[14] * m[3] - m[4] * m[9] * m[14] * m[3] - m[12]
				* m[9] * m[2] * m[7] + m[8] * m[13] * m[2] * m[7] + m[12]
				* m[1] * m[10] * m[7] - m[0] * m[13] * m[10] * m[7] - m[8]
				* m[1] * m[14] * m[7] + m[0] * m[9] * m[14] * m[7] + m[12]
				* m[5] * m[2] * m[11] - m[4] * m[13] * m[2] * m[11] - m[12]
				* m[1] * m[6] * m[11] + m[0] * m[13] * m[6] * m[11] + m[4]
				* m[1] * m[14] * m[11] - m[0] * m[5] * m[14] * m[11] - m[8]
				* m[5] * m[2] * m[15] + m[4] * m[9] * m[2] * m[15] + m[8]
				* m[1] * m[6] * m[15] - m[0] * m[9] * m[6] * m[15] - m[4]
				* m[1] * m[10] * m[15] + m[0] * m[5] * m[10] * m[15];
	}

	public static float[] inverse(float[] m) {
		float x = Determinant4f(m);
		if (x == 0)
			return null;

		float[] i = new float[16];
		i[0] = (-m[13] * m[10] * m[7] + m[9] * m[14] * m[7] + m[13] * m[6]
				* m[11] - m[5] * m[14] * m[11] - m[9] * m[6] * m[15] + m[5]
				* m[10] * m[15])
				/ x;
		i[4] = (m[12] * m[10] * m[7] - m[8] * m[14] * m[7] - m[12] * m[6]
				* m[11] + m[4] * m[14] * m[11] + m[8] * m[6] * m[15] - m[4]
				* m[10] * m[15])
				/ x;
		i[8] = (-m[12] * m[9] * m[7] + m[8] * m[13] * m[7] + m[12] * m[5]
				* m[11] - m[4] * m[13] * m[11] - m[8] * m[5] * m[15] + m[4]
				* m[9] * m[15])
				/ x;
		i[12] = (m[12] * m[9] * m[6] - m[8] * m[13] * m[6] - m[12] * m[5]
				* m[10] + m[4] * m[13] * m[10] + m[8] * m[5] * m[14] - m[4]
				* m[9] * m[14])
				/ x;
		i[1] = (m[13] * m[10] * m[3] - m[9] * m[14] * m[3] - m[13] * m[2]
				* m[11] + m[1] * m[14] * m[11] + m[9] * m[2] * m[15] - m[1]
				* m[10] * m[15])
				/ x;
		i[5] = (-m[12] * m[10] * m[3] + m[8] * m[14] * m[3] + m[12] * m[2]
				* m[11] - m[0] * m[14] * m[11] - m[8] * m[2] * m[15] + m[0]
				* m[10] * m[15])
				/ x;
		i[9] = (m[12] * m[9] * m[3] - m[8] * m[13] * m[3] - m[12] * m[1]
				* m[11] + m[0] * m[13] * m[11] + m[8] * m[1] * m[15] - m[0]
				* m[9] * m[15])
				/ x;
		i[13] = (-m[12] * m[9] * m[2] + m[8] * m[13] * m[2] + m[12] * m[1]
				* m[10] - m[0] * m[13] * m[10] - m[8] * m[1] * m[14] + m[0]
				* m[9] * m[14])
				/ x;
		i[2] = (-m[13] * m[6] * m[3] + m[5] * m[14] * m[3] + m[13] * m[2]
				* m[7] - m[1] * m[14] * m[7] - m[5] * m[2] * m[15] + m[1]
				* m[6] * m[15])
				/ x;
		i[6] = (m[12] * m[6] * m[3] - m[4] * m[14] * m[3] - m[12] * m[2] * m[7]
				+ m[0] * m[14] * m[7] + m[4] * m[2] * m[15] - m[0] * m[6]
				* m[15])
				/ x;
		i[10] = (-m[12] * m[5] * m[3] + m[4] * m[13] * m[3] + m[12] * m[1]
				* m[7] - m[0] * m[13] * m[7] - m[4] * m[1] * m[15] + m[0]
				* m[5] * m[15])
				/ x;
		i[14] = (m[12] * m[5] * m[2] - m[4] * m[13] * m[2] - m[12] * m[1]
				* m[6] + m[0] * m[13] * m[6] + m[4] * m[1] * m[14] - m[0]
				* m[5] * m[14])
				/ x;
		i[3] = (m[9] * m[6] * m[3] - m[5] * m[10] * m[3] - m[9] * m[2] * m[7]
				+ m[1] * m[10] * m[7] + m[5] * m[2] * m[11] - m[1] * m[6]
				* m[11])
				/ x;
		i[7] = (-m[8] * m[6] * m[3] + m[4] * m[10] * m[3] + m[8] * m[2] * m[7]
				- m[0] * m[10] * m[7] - m[4] * m[2] * m[11] + m[0] * m[6]
				* m[11])
				/ x;
		i[11] = (m[8] * m[5] * m[3] - m[4] * m[9] * m[3] - m[8] * m[1] * m[7]
				+ m[0] * m[9] * m[7] + m[4] * m[1] * m[11] - m[0] * m[5]
				* m[11])
				/ x;
		i[15] = (-m[8] * m[5] * m[2] + m[4] * m[9] * m[2] + m[8] * m[1] * m[6]
				- m[0] * m[9] * m[6] - m[4] * m[1] * m[10] + m[0] * m[5]
				* m[10])
				/ x;

		return i;
	}

	public static HVector multiply(float[] m, float x, float y, float z) {
		float w = m[3] + m[7] + m[11] + m[15];
		if (w == 0){
			return null;
		}
		
		return new HVector((m[0] * x + m[4] * y + m[8] * z + m[12]) / w,
				(m[1] * x + m[5] * y + m[9] * z + m[13]) / w,
				(m[2] * x + m[6] * y + m[10] * z + m[14]) / w);
	}
	
	public static boolean multiply(float[] res, float[] m1, float[] m2) {
		if (m1.length != m2.length){
			return false;
		}
		
		res[0] = m1[0] * m2[0] + m1[4] * m2[1] + m1[8] * m2[2] + m1[12] * m2[3];
		res[1] = m1[1] * m2[0] + m1[5] * m2[1] + m1[9] * m2[2] + m1[13] * m2[3];
		res[2] = m1[2] * m2[0] + m1[6] * m2[1] + m1[10] * m2[2] + m1[14] * m2[3];
		res[3] = m1[3] * m2[0] + m1[7] * m2[1] + m1[11] * m2[2] + m1[15] * m2[3];
		res[4] = m1[0] * m2[4] + m1[4] * m2[5] + m1[8] * m2[6] + m1[12] * m2[7];
		res[5] = m1[1] * m2[4] + m1[5] * m2[5] + m1[9] * m2[6] + m1[13] * m2[7];
		res[6] = m1[2] * m2[4] + m1[6] * m2[5] + m1[10] * m2[6] + m1[14] * m2[7];
		res[7] = m1[3] * m2[4] + m1[7] * m2[5] + m1[11] * m2[6] + m1[15] * m2[7];
		res[8] = m1[0] * m2[8] + m1[4] * m2[9] + m1[8] * m2[10] + m1[12] * m2[11];
		res[9] = m1[1] * m2[8] + m1[5] * m2[9] + m1[9] * m2[10] + m1[13] * m2[11];
		res[10] = m1[2] * m2[8] + m1[6] * m2[9] + m1[10] * m2[10] + m1[14] * m2[11];
		res[11] = m1[3] * m2[8] + m1[7] * m2[9] + m1[11] * m2[10] + m1[15] * m2[11];
		res[12] = m1[0] * m2[12] + m1[4] * m2[13] + m1[8] * m2[14] + m1[12] * m2[15];
		res[13] = m1[1] * m2[12] + m1[5] * m2[13] + m1[9] * m2[14] + m1[13] * m2[15];
		res[14] = m1[2] * m2[12] + m1[6] * m2[13] + m1[10] * m2[14] + m1[14] * m2[15];
		res[15] = m1[3] * m2[12] + m1[7] * m2[13] + m1[11] * m2[14] + m1[15] * m2[15];
		return true;
	}
}
