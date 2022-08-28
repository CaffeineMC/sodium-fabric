package net.caffeinemc.sodium.render.entity.shader;

import io.github.douira.glsl_transformer.GLSLParser;
import io.github.douira.glsl_transformer.GLSLParser.*;
import io.github.douira.glsl_transformer.ast.Tensor;
import io.github.douira.glsl_transformer.ast.Type;
import io.github.douira.glsl_transformer.core.SemanticException;
import io.github.douira.glsl_transformer.print.filter.ChannelFilter;
import io.github.douira.glsl_transformer.print.filter.TokenChannel;
import io.github.douira.glsl_transformer.print.filter.TokenFilter;
import io.github.douira.glsl_transformer.transform.*;

import java.util.Locale;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreeMatch;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

// TODO: maybe make this an interface with static constants?
public class ShaderTransformer {

    public static final TransformationManager<EntityParameters> ENTITY = createEntityTransformationManager();

    private static final String PREFIX = "sodium_";
    private static final int REQUIRED_VERSION = 330;

    private static final String VERTEX_INPUT_QUALIFIER = "in";
    private static final String UNIFORM_QUALIFIER = "uniform";

    private static TransformationManager<EntityParameters> createEntityTransformationManager() {

        TokenFilter<EntityParameters> parseTokenFilter = new ChannelFilter<>(TokenChannel.PREPROCESSOR) {
            private static final Set<String> DISALLOWED_PP_TOKENS = Set.of(
                    "#IF",
                    "#IFDEF",
                    "#IFNDEF",
                    "#ELSE",
                    "#ELIF",
                    "#ENDIF"
            );

            @Override
            public boolean isTokenAllowed(Token token) {
                if (!super.isTokenAllowed(token) && DISALLOWED_PP_TOKENS.contains(token.getText().toUpperCase(Locale.ROOT))) {
                    throw new SemanticException("Unparsed preprocessor directives such as '" + token.getText()
                            + "' may not be present at this stage of shader processing!");
                }
                return true;
            }
        };

        // need atleast 330 core
        TransformationPhase<EntityParameters> fixVersion = new RunPhase<>() {
            @Override
            protected void run(TranslationUnitContext ctx) {
                VersionStatementContext versionStatement = ctx.versionStatement();
                if (versionStatement == null) {
                    throw new IllegalStateException("Missing the version statement!");
                }

                int version = Integer.parseInt(versionStatement.NR_INTCONSTANT().getText());

                if (version < REQUIRED_VERSION) {
                    version = REQUIRED_VERSION;
                    // we shouldn't need to specify core, but i will just in case
                    replaceNode(versionStatement, "#version " + version + " core" + "\n",
                            GLSLParser::versionStatement);
                }
            }
        };

        TransformationPhase<EntityParameters> addExtensions = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_EXTENSIONS,
                "#extension GL_ARB_shader_storage_buffer_object : require\n"
        );

        LifecycleUser<EntityParameters> replaceVertexInputs = new Transformation<>() {
            private final MutableBoolean foundNormal = new MutableBoolean(false);
            private final MutableBoolean foundColor = new MutableBoolean(false);
            private final MutableBoolean foundUV1 = new MutableBoolean(false);
            private final MutableBoolean foundUV2 = new MutableBoolean(false);

            @Override
            public void resetState() {
                this.foundNormal.setFalse();
                this.foundColor.setFalse();
                this.foundUV1.setFalse();
                this.foundUV2.setFalse();
            }

            @Override
            protected void setupGraph() {
                // Color
                if (!this.getJobParameters().skippedVertexInputs.contains("Color")) {
                    ReplaceInputPhase<EntityParameters> replaceColor = new ReplaceInputPhase<>(
                            VERTEX_INPUT_QUALIFIER,
                            Type.F32VEC4,
                            "Color",
                            this.foundNormal
                    );
                    this.addEndDependent(replaceColor);

                    this.chainDependent(new AddToMainHeadPhase<>() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            return createLocalRoot("\n    Color = " + PREFIX + "model.Color;", methodBody, GLSLParser::statement);
                        }
                    }).activation(this.foundNormal::booleanValue);
                }

                // UV1
                if (!this.getJobParameters().skippedVertexInputs.contains("UV1")) {
                    ReplaceInputPhase<EntityParameters> replaceUV1 = new ReplaceInputPhase<>(
                            VERTEX_INPUT_QUALIFIER,
                            Type.I16VEC2,
                            "UV1",
                            this.foundUV1
                    );
                    this.addEndDependent(replaceUV1);

                    this.chainDependent(new AddToMainHeadPhase<>() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            return createLocalRoot("\n    UV1 = " + PREFIX + "model.UV1;", methodBody, GLSLParser::statement);
                        }
                    }).activation(this.foundUV1::booleanValue);
                }

                // UV2
                if (!this.getJobParameters().skippedVertexInputs.contains("UV2")) {
                    ReplaceInputPhase<EntityParameters> replaceUV2 = new ReplaceInputPhase<>(
                            VERTEX_INPUT_QUALIFIER,
                            Type.I16VEC2,
                            "UV2",
                            this.foundUV2
                    );
                    this.addEndDependent(replaceUV2);

                    this.chainDependent(new AddToMainHeadPhase<>() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            return createLocalRoot("\n    UV2 = " + PREFIX + "model.UV2;", methodBody, GLSLParser::statement);
                        }
                    }).activation(this.foundUV2::booleanValue);
                }

                // Normal
                if (!this.getJobParameters().skippedVertexInputs.contains("Normal")) {
                    ReplaceInputPhase<EntityParameters> replaceNormal = new ReplaceInputPhase<>(
                            VERTEX_INPUT_QUALIFIER,
                            Type.I8VEC3,
                            "Normal",
                            "\nin vec3 " + PREFIX + "PreMulNormal;",
                            this.foundNormal
                    );

                    this.addEndDependent(replaceNormal);

                    this.chainDependent(new AddToMainHeadPhase<>() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            // does this actually need another call to normalize?
                            return createLocalRoot(
                                    "\n    Normal = normalize(mat3(" + PREFIX + "modelPart.NormalMat) * " + PREFIX + "PreMulNormal);",
                                    methodBody,
                                    GLSLParser::statement
                            );
                        }
                    }).activation(this.foundNormal::booleanValue);
                }
            }
        };

        TransformationPhase<EntityParameters> addVertexInputs = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                "\nin uint " + PREFIX + "PartId;"
        );

        TransformationPhase<EntityParameters> addUniforms = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                "\nuniform int " + PREFIX + "InstanceOffset;"
        );

        Transformation<EntityParameters> retrieveModelInstanceAndModelPart = new Transformation<>() {
            @Override
            protected void setupGraph() {
                this.addEndDependent(new AddToMainHeadPhase<>() {
                    @Override
                    protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                        return createLocalRoot(
                                "\n    ModelPart " + PREFIX + "modelPart = " + PREFIX + "ModelPartsBuffer.modelParts[" + PREFIX + "modelInstance.PartOffset + " + PREFIX + "PartId];",
                                methodBody,
                                GLSLParser::statement
                        );
                    }
                });

                this.chainDependent(new AddToMainHeadPhase<>() {
                    @Override
                    protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                        return createLocalRoot(
                                "\n    ModelInstance " + PREFIX + "modelInstance = " + PREFIX + "ModelInstancesBuffer.modelInstances[" + PREFIX + "InstanceOffset + gl_InstanceID];",
                                methodBody,
                                GLSLParser::statement
                        );
                    }
                });
            }
        };

        TransformationPhase<EntityParameters> createBuffers = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                "\n" +
                        "layout(std140, binding = 1) readonly restrict buffer " + PREFIX + "ModelPartsLayout {\n" +
                        "    ModelPart[] modelParts;\n" +
                        "} " + PREFIX + "ModelPartsBuffer;",
                "\n" +
                        "layout(std140, binding = 2) readonly restrict buffer " + PREFIX + "ModelInstancesLayout {\n" +
                        "    ModelInstance[] ModelInstances;\n" +
                        "} " + PREFIX + "ModelInstancesBuffer;"
        );

        Transformation<EntityParameters> createModelPartStruct = new Transformation<>() {
            private final MutableBoolean foundModelViewMat = new MutableBoolean(false);

            @Override
            public void resetState() {
                this.foundModelViewMat.setFalse();
            }

            @Override
            protected void setupGraph() {
                // TODO: allow adding more to this and dynamically removing stuff
                if (!this.getJobParameters().skippedUniforms.contains("ModelViewMat")) {
                    ReplaceInputPhase<EntityParameters> replaceModelViewMat = new ReplaceInputPhase<>(
                            UNIFORM_QUALIFIER,
                            Type.I8VEC3,
                            "ModelViewMat",
                            this.foundModelViewMat
                    );

                    this.addEndDependent(replaceModelViewMat);

                    this.chainDependent(new AddToMainHeadPhase<>() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            // does this actually need another call to normalize?
                            return createLocalRoot(
                                    "\n    ModelViewMat = " + PREFIX + "modelPart.ModelViewMat;",
                                    methodBody,
                                    GLSLParser::statement
                            );
                        }
                    }).activation(this.foundModelViewMat::booleanValue);
                }

                this.chainDependent(RunPhase.withInjectExternalDeclarations(
                                InjectionPoint.BEFORE_FUNCTIONS,
                                """
                                        struct ModelPart {
                                            mat4 ModelViewMat;
                                            mat3x4 NormalMat;
                                        };
                                        """
                        )
                );
            }
        };

        // TODO: allow adding more to this and dynamically removing stuff
        TransformationPhase<EntityParameters> createModelInstanceStruct = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                """
                                                
                        struct ModelInstance {
                            vec4 Color;
                            ivec2 UV1;
                            ivec2 UV2;
                            vec3 Padding;
                            uint PartOffset;
                        };
                        """
        );

        TransformationManager<EntityParameters> manager = new TransformationManager<>(new Transformation<>() {
            @Override
            protected void setupGraph() {
                this.addEndDependent(fixVersion);
                this.chainDependent(addExtensions);
                this.chainDependent(replaceVertexInputs);
                this.chainDependent(addVertexInputs);
                this.chainDependent(addUniforms);
                this.chainDependent(createModelInstanceStruct);
                this.chainDependent(createModelPartStruct);
                this.chainDependent(createBuffers);
                this.chainDependent(retrieveModelInstanceAndModelPart);
            }
        });

        manager.setParseTokenFilter(parseTokenFilter);

        return manager;
    }

    public static class EntityParameters extends JobParameters {
        private final Set<String> skippedVertexInputs;
        private final Set<String> skippedUniforms;

        public EntityParameters() {
            this(Set.of(), Set.of());
        }

        public EntityParameters(Set<String> skippedVertexInputs, Set<String> skippedUniforms) {
            // TODO: add parameters for adding custom vert inputs / uniforms to the buffers
            // uniforms should probably be put into per model instance by default, vertex inputs should stay out by default
            // TODO: add a mode switch for vert divisor vs buffer instancing, add a mode switch for translucency w/ vertex pulling
            // TODO: add a boolean for if vulkan is being used or not
            this.skippedVertexInputs = skippedVertexInputs;
            this.skippedUniforms = skippedUniforms;
        }

        @Override
        public boolean equals(JobParameters o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            EntityParameters that = (EntityParameters) o;
            return this.skippedVertexInputs.equals(that.skippedVertexInputs) && this.skippedUniforms.equals(that.skippedUniforms);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.skippedVertexInputs, this.skippedUniforms);
        }
    }

    private static abstract class AddToMainHeadPhase<T extends JobParameters> extends WalkPhase<T> {
        protected ParseTreePattern mainFunctionPattern;

        @Override
        public void init() {
            this.mainFunctionPattern = this.compilePattern("void main() <body:compoundStatement>", GLSLParser.RULE_functionDefinition);
        }

        @Override
        public void enterFunctionDefinition(FunctionDefinitionContext ctx) {
            ParseTreeMatch match = this.mainFunctionPattern.match(ctx);
            if (match.succeeded()) {
                CompoundStatementContext methodBody = (CompoundStatementContext) match.get("body");
                ParseTree normalDefinition = this.createNodeToAdd(methodBody);
                methodBody.addChild(
                        1, // we need to inject after the first brace, which is guaranteed to be in index 0
                        normalDefinition
                );
            }
        }

        protected abstract ParseTree createNodeToAdd(CompoundStatementContext methodBody);
    }

    private static class ReplaceInputPhase<T extends JobParameters> extends WalkPhase<T> {
        private final String qualifier;
        private final Type type;
        private final String name;
        private final String replacement;
        private final MutableBoolean found;

        private ParseTreePattern vertInputPattern;

        public ReplaceInputPhase(String qualifier, Type type, String name, MutableBoolean found) {
            this(qualifier, type, name, null, found);
        }

        public ReplaceInputPhase(String qualifier, Type type, String name, @Nullable String replacement, MutableBoolean found) {
            this.qualifier = qualifier;
            this.type = type;
            this.name = name;
            this.replacement = replacement;
            this.found = found;
        }

        @Override
        public void init() {
            this.vertInputPattern = this.compilePattern(this.qualifier + " <type:builtinTypeSpecifierParseable> " + this.name + ";", GLSLParser.RULE_externalDeclaration);
        }

        @Override
        public void enterExternalDeclaration(ExternalDeclarationContext ctx) {
            ParseTreeMatch match = this.vertInputPattern.match(ctx);
            if (match.succeeded()) {
                BuiltinTypeSpecifierParseableContext typeContext = (BuiltinTypeSpecifierParseableContext) match.get("type");
                Type parsedType = new Tensor(typeContext).getType();
                if (this.type.getImplicitCasts().contains(parsedType)) {
                    if (this.replacement != null) {
                        replaceNode(ctx, this.replacement, GLSLParser::externalDeclaration);
                    } else {
                        removeNode(ctx);
                    }
                    this.injectExternalDeclaration(InjectionPoint.BEFORE_FUNCTIONS, "\n" + typeContext.getText() + " " + this.name + ";");
                    this.found.setTrue();
                }
            }
        }

        @Override
        public boolean canStop() {
            return super.canStop() && this.found.getValue();
        }
    }
}
