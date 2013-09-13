package com.dwtech.android.launcher3d;

import android.opengl.GLES20;

public class ShaderManager {
    final static String vs_bg =
            "uniform mat4 u_MVPMatrix;      \n"     // A constant representing the combined model/view/projection matrix.
         
          + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
          + "attribute vec4 a_Texcoord;     \n"
         
          + "varying vec3 pos;              \n"
          + "varying vec2 texcoords;        \n"
         
          + "void main()                    \n"     // The entry point for our vertex shader.
          + "{                              \n"
          + "   pos = a_Position.xyz;        \n"
          + "   texcoords.st = a_Texcoord.st;          \n"
          + "   gl_Position = a_Position;   \n"     // gl_Position is a special variable used to store the final position.
          + "                  \n"     // Multiply the vertex by the matrix to get the final point in
          + "}                              \n";
    
    final static String fs_bg =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
                                                    // precision in the fragment shader.
          + "uniform samplerExternalOES tex;         \n"
          
          + "varying vec3 pos;              \n"
          + "varying vec2 texcoords;        \n"
          
          + "void main()                    \n"     // The entry point for our fragment shader.
          + "{                              \n"
          + "   //float d = 1.5 - length(pos);\n"
          + "   vec2 tc = texcoords;  \n"
          + "   vec4 texColor = texture2D(tex, tc);\n"
          + "   gl_FragColor = texColor;     \n"     // Pass the color directly through the pipeline.
          + "}                              \n"
          ;
    
    final static String vs_barrel =
            "uniform mat4 u_MVPMatrix;      \n"     // A constant representing the combined model/view/projection matrix.
         
          + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
          + "attribute vec4 a_Texcoord;     \n"
         
          + "varying vec3 pos;              \n"
          + "varying vec2 texcoords;        \n"
         
          + "void main()                    \n"     // The entry point for our vertex shader.
          + "{                              \n"
          + "   pos = a_Position.xyz;        \n"
          + "   texcoords.st = a_Texcoord.st;          \n"
                                                    // It will be interpolated across the triangle.
          + "   gl_Position = u_MVPMatrix * a_Position;   \n"     // gl_Position is a special variable used to store the final position.
          + "                  \n"     // Multiply the vertex by the matrix to get the final point in
          + "}                              \n";
    
    final static String fs_barrel =
            "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
                                                    // precision in the fragment shader.
          + "uniform sampler2D tex;         \n"
          + "uniform sampler2D pretex;         \n"
          + "uniform sampler2D pretex2;         \n"
          + "uniform samplerExternalOES tex2;         \n"
          + "uniform float uBarrelLevel;         \n"
          + "uniform int uHWacc;         \n"
          + "uniform vec2 uTCScale;         \n"
          + "uniform int uIsBg;         \n"
          
          + "varying vec3 pos;              \n"
          + "varying vec2 texcoords;        \n"
          
            + "vec2 Distort(vec2 p)           \n"
            + "{                              \n"
            + "   float theta  = atan(p.y, p.x);\n"
            + "   float radius = length(p);\n"
            + "   radius = pow(radius, uBarrelLevel);\n"
            + "   p.x = radius * cos(theta);  \n"
            + "   p.y = radius * sin(theta);  \n"
            + "   return 0.5 * (p + 1.0);     \n"
            + "}                              \n"

          + "void main()                    \n"     // The entry point for our fragment shader.
          + "{                              \n"
          + "   //float d = 1.5 - length(pos);\n"
          + "   vec2 tc = vec2(0.0, 0.0);      \n"
          + "   if (uHWacc == 1) {               \n"
          + "   	//if (uIsBg == 0) {               \n"
          + "       	//tc = Distort(pos.xy);  \n"
          + "   	//} else {                    \n"
          + "       	tc.s = texcoords.s;\n"
          + "       	tc.t = 1.0 - texcoords.t;\n"
          + "		//}									\n"
          + "   } else {                    \n"
          + "       vec4 pretexColor = texture2D(pretex, texcoords);\n"
          + "       tc.x = pretexColor.r;// + pretexColor.b / 256.0;//mostsig4;// / 256.0 + pretexColor.b / 65536.0;\n"  
          + "       tc.y = pretexColor.g;// + lestsig4;//pretexColor2.g / 256.0 + pretexColor2.b / 65536.0;\n"
          + "   }                           \n"   
          + "   tc.x *= uTCScale.x;            \n"
          + "   tc.y = (1.0-tc.y) * uTCScale.y;\n"
          
          + "   if (tc.s < 0.0 || tc.s > 1.0 || tc.t < 0.0 || tc.t > 1.0)   \n"
          + "   {                                                           \n"
          + "       gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);                \n"
          + "   } else if (uIsBg == 1) {                     \n"
          + "       vec4 texColor = texture2D(tex2, vec2(0.8-tc.s * 0.6, 1.0-tc.t));\n" // we should scale 0.5, however on S4 it is stretched
          + "       gl_FragColor = texColor;                                \n"
          + "   } else {                                                    \n"
          + "       vec4 texColor = texture2D(tex, tc);                     \n"
          + "       gl_FragColor = texColor;                                \n"
          + "   }                                                           \n"
          + "}                                \n"
          ;
    
    final static String vs_oculus_rift =
            "uniform mat4 u_MVPMatrix;      \n"     // A constant representing the combined model/view/projection matrix.
         
          + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
          + "attribute vec4 a_Texcoord;     \n"
         
          + "varying vec2 texcoords;        \n"
         
          + "void main()                    \n"     // The entry point for our vertex shader.
          + "{                              \n"
          + "   texcoords.st = a_Texcoord.st;          \n"
          + "   gl_Position = u_MVPMatrix * a_Position;   \n"     // gl_Position is a special variable used to store the final position.
          + "                  \n"     // Multiply the vertex by the matrix to get the final point in
          + "}                              \n";
    
    final static String fs_oculus_rift =
            "#extension GL_OES_EGL_image_external : require\n"
          + "precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
                                                    // precision in the fragment shader.
          + "uniform sampler2D tex;         \n"
          + "uniform samplerExternalOES tex2;         \n"
          + "uniform sampler2D pretex;         \n"
          + "uniform vec4 HmdWarpParam;     \n"
          + "uniform vec2 uTCScale;         \n"
          + "uniform int uIsBg;         \n"
          + "uniform float uScale;         \n"
          + "uniform int uHWacc;         \n"
          
          + "varying vec2 texcoords;        \n"
          
         + "vec2 HmdWarp(vec2 in01)           \n"
         + "{                              \n"
         + "    vec2 lensCenter = vec2(0.5, 0.5) * uTCScale;"
         + "    vec2 theta = (in01 - lensCenter) * 2.0;\n"
         + "    float rSq = theta.x * theta.x + theta.y * theta.y;\n"
         + "    vec2 rvector = theta * (HmdWarpParam.x + HmdWarpParam.y * rSq + HmdWarpParam.z * rSq * rSq + HmdWarpParam.w * rSq * rSq * rSq);\n"
         + "    return lensCenter + uScale * rvector;  \n"
         + "} "

          + "void main()                                                    \n" 
          + "{                                                              \n"
          + "   vec2 tc = vec2(0.0, 0.0);      \n"
          + "   if (uHWacc == 1) {               \n"
          + "       tc = HmdWarp(texcoords * uTCScale);\n"
          + "   } else {                    \n"
          + "       vec4 pretexColor = texture2D(pretex, texcoords);\n"
          + "       tc.x = pretexColor.r;// + pretexColor.b / 256.0;//mostsig4;// / 256.0 + pretexColor.b / 65536.0;\n"  
          + "       tc.y = pretexColor.g;// + lestsig4;//pretexColor2.g / 256.0 + pretexColor2.b / 65536.0;\n"
          + "   }                                                           \n"         
          + "   if (tc.s < 0.0 || tc.s > 1.0 || tc.t < 0.0 || tc.t > 1.0) {  \n"
          + "       gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);                \n"
          + "   } else if (uIsBg == 1) {                     \n"
          + "       vec4 texColor = texture2D(tex2, vec2(0.8-tc.s * 0.6, 1.0-tc.t));\n" // we should scale 0.5, however on S4 it is stretched
          + "       gl_FragColor = texColor;                                \n"
          + "   } else {                                                    \n"
          + "       vec4 texColor = texture2D(tex, tc);                     \n"
          + "       gl_FragColor = texColor;                                \n"
          + "   }                                                           \n"
          + "}                                                              \n"
          ;
    
    public static final int SDR_BG = 0;
    public static final int SDR_BARREL = 1;
    public static final int SDR_OCULUS_RIFT = 2;
    
    public static int createShader(int type) {
        String vs = "";
        String fs = "";
        
        switch(type) {
            case SDR_BG:
                vs = vs_bg;
                fs = fs_bg;
                break;
                
            case SDR_BARREL:
                vs = vs_barrel;
                fs = fs_barrel;
                break;
                
            case SDR_OCULUS_RIFT:
                vs = vs_oculus_rift;
                fs = fs_oculus_rift;
                break;
        }
        
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (vertexShaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vs);

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }
        
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fs);

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }
        
        int programHandle = GLES20.glCreateProgram();
        
        if (programHandle != 0)
        {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
         
            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);
         
            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Texcoord");
         
            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);
         
            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
         
            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }
        
        return programHandle;
    }
}
