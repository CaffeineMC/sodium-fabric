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
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreeMatch;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

public class EntityShaderTransformer {
    private static final TransformationManager<Parameters> MANAGER;
    private static final String PREFIX = "sodium_";
    private static final int REQUIRED_VERSION = 330;

    private static class Parameters extends JobParameters {
        private final Set<String> skippedVertexInputs;
        private final Set<String> skippedUniforms;

        public Parameters() {
            this(Set.of(), Set.of());
        }

        public Parameters(Set<String> skippedVertexInputs, Set<String> skippedUniforms) {
            // TODO: add parameters for adding custom vert inputs / uniforms to the buffers
            // uniforms should probably be put into per model by default, vertex inputs should stay out by default
            // TODO: add a mode switch for vert divisor vs buffer instancing, add a mode switch for translucency w/ vertex pulling
            // TODO: add a boolean for if vulkan is being used or not
            this.skippedVertexInputs = skippedVertexInputs;
            this.skippedUniforms = skippedUniforms;
        }

        @Override
        public boolean equals(JobParameters o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            Parameters that = (Parameters) o;
            return this.skippedVertexInputs.equals(that.skippedVertexInputs) && this.skippedUniforms.equals(that.skippedUniforms);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.skippedVertexInputs, this.skippedUniforms);
        }
    }

    private static abstract class AddToMainHeadPhase extends WalkPhase<Parameters> {
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

    private static class ReplaceVertexInputPhase extends WalkPhase<Parameters> {
        private final Type type;
        private final String name;
        private final String replacement;
        private final MutableBoolean found;

        private ParseTreePattern vertInputPattern;

        public ReplaceVertexInputPhase(Type type, String name, MutableBoolean found) {
            this(type, name, null, found);
        }

        public ReplaceVertexInputPhase(Type type, String name, @Nullable String replacement, MutableBoolean found) {
            this.type = type;
            this.name = name;
            this.replacement = replacement;
            this.found = found;
        }

        @Override
        public void init() {
            this.vertInputPattern = this.compilePattern("in <type:builtinTypeSpecifierParseable> " + this.name + ";", GLSLParser.RULE_externalDeclaration);
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
        public boolean isFinished() {
            return this.found.getValue();
        }
    }

    static {

        TokenFilter<Parameters> parseTokenFilter = new ChannelFilter<>(TokenChannel.PREPROCESSOR) {
            @Override
            public boolean isTokenAllowed(Token token) {
                if (!super.isTokenAllowed(token)) {
                    throw new SemanticException("Unparsed preprocessor directives such as '" + token.getText()
                            + "' may not be present at this stage of shader processing!");
                }
                return true;
            }
        };

        // need atleast 330 core
        TransformationPhase<Parameters> fixVersion = new RunPhase<>() {
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

        TransformationPhase<Parameters> addExtensions = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_EXTENSIONS,
                "#extension GL_ARB_shader_storage_buffer_object : require\n"
        );

        LifecycleUser<Parameters> replaceVertexInputs = new Transformation<>() {
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
                    ReplaceVertexInputPhase replaceColor = new ReplaceVertexInputPhase(Type.F32VEC4, "Color", this.foundNormal);
                    this.addEndDependent(replaceColor);

                    this.chainDependent(new AddToMainHeadPhase() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            return createLocalRoot("\n    Color = " + PREFIX + "model.Color;", methodBody, GLSLParser::statement);
                        }
                    }).activation(this.foundNormal::booleanValue);
                }

                // UV1
                if (!this.getJobParameters().skippedVertexInputs.contains("UV1")) {
                    ReplaceVertexInputPhase replaceUV1 = new ReplaceVertexInputPhase(Type.I16VEC2, "UV1", this.foundUV1);
                    this.addEndDependent(replaceUV1);

                    this.chainDependent(new AddToMainHeadPhase() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            return createLocalRoot("\n    UV1 = " + PREFIX + "model.UV1;", methodBody, GLSLParser::statement);
                        }
                    }).activation(this.foundUV1::booleanValue);
                }

                // UV2
                if (!this.getJobParameters().skippedVertexInputs.contains("UV2")) {
                    ReplaceVertexInputPhase replaceUV2 = new ReplaceVertexInputPhase(Type.I16VEC2, "UV2", this.foundUV2);
                    this.addEndDependent(replaceUV2);

                    this.chainDependent(new AddToMainHeadPhase() {
                        @Override
                        protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                            return createLocalRoot("\n    UV2 = " + PREFIX + "model.UV2;", methodBody, GLSLParser::statement);
                        }
                    }).activation(this.foundUV2::booleanValue);
                }

                // Normal
                if (!this.getJobParameters().skippedVertexInputs.contains("Normal")) {
                    ReplaceVertexInputPhase replaceNormal = new ReplaceVertexInputPhase(
                            Type.I8VEC3,
                            "Normal",
                            "\nin vec3 " + PREFIX + "PreMulNormal;",
                            this.foundNormal
                    );

                    this.addEndDependent(replaceNormal);

                    this.chainDependent(new AddToMainHeadPhase() {
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

        TransformationPhase<Parameters> addVertexInputs = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                "\nin uint " + PREFIX + "PartId;"
        );

        TransformationPhase<Parameters> addUniforms = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                "\nuniform uint " + PREFIX + "InstanceOffset;"
        );

        Transformation<Parameters> retrieveModelAndModelPart = new Transformation<>() {
            @Override
            protected void setupGraph() {
                this.addEndDependent(new AddToMainHeadPhase() {
                    @Override
                    protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                        return createLocalRoot(
                                "\n    Model " + PREFIX + "model = " + PREFIX + "ModelsSsbo.models[" + PREFIX + "InstanceOffset + gl_InstanceID];",
                                methodBody,
                                GLSLParser::statement
                        );
                    }
                });

                this.chainDependent(new AddToMainHeadPhase() {
                    @Override
                    protected ParseTree createNodeToAdd(CompoundStatementContext methodBody) {
                        return createLocalRoot(
                                "\n    ModelPart " + PREFIX + "modelPart = " + PREFIX + "ModelPartsSsbo.modelParts[" + PREFIX + "model.PartOffset + " + PREFIX + "PartId];",
                                methodBody,
                                GLSLParser::statement
                        );
                    }
                });
            }
        };

        TransformationPhase<Parameters> createBuffers = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                "\n" +
                        "layout(std140, binding = 1) readonly restrict buffer " + PREFIX + "ModelPartsLayout {\n" +
                        "    ModelPart[] modelParts;\n" +
                        "} " + PREFIX + "ModelPartsBuffer;",
                "\n" +
                        "layout(std140, binding = 2) readonly restrict buffer " + PREFIX + "ModelsLayout {\n" +
                        "    Model[] models;\n" +
                        "} " + PREFIX + "ModelsBuffer;"
        );

        Transformation<Parameters> createModelPartStruct = new Transformation<>() {
            @Override
            protected void setupGraph() {
                // TODO: allow adding more to this and dynamically removing stuff
                if (!this.getJobParameters().skippedUniforms.contains("ModelViewMat")) {
                    this.addEndDependent(new WalkPhase<Parameters>() {
                        private ParseTreePattern modelViewMatPattern;

                        @Override
                        public void init() {
                            this.modelViewMatPattern = this.compilePattern("uniform mat4 ModelViewMat;", GLSLParser.RULE_externalDeclaration);
                        }

                        @Override
                        public void enterExternalDeclaration(ExternalDeclarationContext ctx) {
                            ParseTreeMatch match = this.modelViewMatPattern.match(ctx);
                            if (match.succeeded()) {
                                removeNode(ctx);
                            }
                        }
                    });
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
        TransformationPhase<Parameters> createModelStruct = RunPhase.withInjectExternalDeclarations(
                InjectionPoint.BEFORE_FUNCTIONS,
                """
                                                
                        struct Model {
                            vec4 Color;
                            ivec2 UV1;
                            ivec2 UV2;
                            vec3 Padding;
                            uint PartOffset;
                        };
                        """
        );

        MANAGER = new TransformationManager<>(new Transformation<>() {
            @Override
            protected void setupGraph() {
                this.addEndDependent(fixVersion);
                this.chainDependent(addExtensions);
                this.chainDependent(replaceVertexInputs);
                this.chainDependent(addVertexInputs);
                this.chainDependent(addUniforms);
                this.chainDependent(createModelStruct);
                this.chainDependent(createModelPartStruct);
                this.chainDependent(createBuffers);
                this.chainDependent(retrieveModelAndModelPart);
            }
        });

//        MANAGER.setParseTokenFilter(parseTokenFilter);
    }

    private static String transform(String source, Parameters parameters) {
        return MANAGER.transform(source, parameters);
    }

    public static void main(String[] args) {
        System.out.println(
                transform(
                        """
                                #version 150
                                                        
                                //#moj_import <light.glsl>
                                //#moj_import <fog.glsl>
                                                        
                                in vec3 Position;
                                in vec4 Color;
                                in vec2 UV0;
                                in ivec2 UV1;
                                in ivec2 UV2;
                                in vec3 Normal;
                                                        
                                uniform sampler2D Sampler1;
                                uniform sampler2D Sampler2;
                                                        
                                uniform mat4 ModelViewMat;
                                uniform mat4 ProjMat;
                                uniform mat3 IViewRotMat;
                                uniform int FogShape;
                                                        
                                uniform vec3 Light0_Direction;
                                uniform vec3 Light1_Direction;
                                                        
                                out float vertexDistance;
                                out vec4 vertexColor;
                                out vec4 lightMapColor;
                                out vec4 overlayColor;
                                out vec2 texCoord0;
                                out vec4 normal;
                                                        
                                void main() {
                                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                                                                
                                    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
                                    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
                                    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
                                    overlayColor = texelFetch(Sampler1, UV1, 0);
                                    texCoord0 = UV0;
                                    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
                                }
                                """,
                        new Parameters()
                )
        );
    }
}
