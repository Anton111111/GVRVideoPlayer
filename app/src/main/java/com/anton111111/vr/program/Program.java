package com.anton111111.vr.program;

import com.anton111111.vr.GLHelper;

public class Program {

    private int programId;
    private GLHelper.ShaderAttributesLocations shaderAttributeLocations;

    public int getProgramId() {
        return programId;
    }

    public GLHelper.ShaderAttributesLocations getShaderAttributeLocations() {
        return shaderAttributeLocations;
    }

    public Program(int programId, GLHelper.ShaderAttributesLocations shaderAttributeLocations) {
        this.programId = programId;
        this.shaderAttributeLocations = shaderAttributeLocations;
    }

    public int getAttr(String name) {
        return shaderAttributeLocations.getAttr(name);
    }

    public int getUniform(String name) {
        return shaderAttributeLocations.getUniform(name);
    }

}
