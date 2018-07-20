#extension GL_OES_EGL_image_external : require
precision highp float;

#define M_PI 3.1415926535897932384626433832795

varying vec3 v_Position;

uniform samplerExternalOES u_Texture;
uniform float u_Fov;
uniform float u_TextureCordOffset;
uniform int u_StereoType;
uniform mat4 u_TTM;

void main() {
	float vAngle = acos(v_Position.y / length(v_Position));
	//turn the angle to vertical flip texture
	vAngle = M_PI - vAngle;
	float hAngle = atan(v_Position.x, -v_Position.z);

	float maxRotateAngle = u_Fov / 2.0;
	if (abs(hAngle) > maxRotateAngle){
       	discard;
    } else {
	    float X = (maxRotateAngle + hAngle) / u_Fov;
	    float Y = vAngle / M_PI;
        if (u_StereoType == 1) {
            X = (X / 2.0) + u_TextureCordOffset;
        } else if (u_StereoType == 2) {
            Y = (Y / 2.0) + u_TextureCordOffset;
        }

        vec2 uv = (u_TTM * vec4(X, Y, 0.0, 1.0)).xy;
        gl_FragColor = texture2D(u_Texture, uv);
    }
}
