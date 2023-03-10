/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

/**
 * @author thomas
 */
public final class SignatureArrayTypeModifier {

    @Deprecated
    public static String getNewSignatureField(String signature) {
        String newSignature = signature;
        if (signature != null) {
            SignatureReader sr = new SignatureReader(signature);
            ArrayRewriteSignatureVisitor arsv = new ArrayRewriteSignatureVisitor();
            sr.acceptType(arsv);
            newSignature = arsv.toString();
        }
        return newSignature;
    }

    /**
     * Takes a signature / description and returns it with arrays being replaced by arrayWrapper accordingly of the type of the array.
     *
     * @param signature A signature - description.
     * @return A similar signature / description with ArrayWrapper instead of arrays.
     */
    public static String getNewSignature(String signature) {
        String newSignature = signature;
        if (signature != null) {
            SignatureReader sr = new SignatureReader(signature);
            ArrayRewriteSignatureVisitorTest arsv = new ArrayRewriteSignatureVisitorTest();
            sr.accept(arsv);
            newSignature = arsv.toString();
        }
        return newSignature;
    }

    /**
     * SignatureVisitor Class that will replaces every array subtype with appropriate Class of ArrayWrapper.
     */
    private static final class ArrayRewriteSignatureVisitor extends SignatureWriter {

        private String arrayWrapperName = PathConstants.OBJECT_ARRAY_WRAPPER_NAME;

        public ArrayRewriteSignatureVisitor() {
            super();
        }

        @Override
        public SignatureVisitor visitArrayType() {
            SignatureWriter refToThis = this;
            class ArraySignatureVisitor extends SignatureWriter {

                public ArraySignatureVisitor(int api) {
                    super();
                }

                private boolean isArrayVisited = false;


                @Override
                public void visitFormalTypeParameter(String name) {
                    super.visitFormalTypeParameter(name);
                }

                @Override
                public SignatureVisitor visitClassBound() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitClassBound();
                }

                @Override
                public SignatureVisitor visitInterfaceBound() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitInterfaceBound();
                }

                @Override
                public SignatureVisitor visitSuperclass() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitSuperclass();
                }

                @Override
                public SignatureVisitor visitInterface() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitInterface();
                }

                @Override
                public SignatureVisitor visitParameterType() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitParameterType();
                }

                @Override
                public SignatureVisitor visitReturnType() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitReturnType();
                }

                @Override
                public SignatureVisitor visitExceptionType() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitExceptionType();
                }

                @Override
                public void visitBaseType(char c) {
                    if (!isArrayVisited) {
                        refToThis.visitClassType("xyz/acygn/millr/util/" + Type.getType(String.valueOf(c)).getClassName() + "ArrayWrapper");
                        refToThis.visitEnd();
                        return;
                    }
                    super.visitBaseType(c);
                    isArrayVisited = true;


                }

                @Override
                public SignatureVisitor visitArrayType() {
                    if (!isArrayVisited) {
                        refToThis.visitClassType(arrayWrapperName);
                        refToThis.visitEnd();
                        isArrayVisited = true;
                    }
                    return new SignatureVisitor(Opcodes.ASM6) {
                    };

                }

                @Override
                public void visitClassType(String name) {
                    if (!isArrayVisited) {
                        refToThis.visitClassType(arrayWrapperName);
                        refToThis.visitEnd();
                        isArrayVisited = true;
                    }
                }

                @Override
                public void visitInnerClassType(String name) {
                    super.visitInnerClassType(name);
                }

                @Override
                public void visitTypeArgument() {
                    super.visitTypeArgument();
                }

                @Override
                public SignatureVisitor visitTypeArgument(char wildcard) {
                    return super.visitTypeArgument(wildcard);
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                }

                @Override
                public String toString() {
                    return super.toString();
                }

                @Override
                public void visitTypeVariable(String name) {
                    if (!isArrayVisited) {
                        refToThis.visitClassType(arrayWrapperName);
                        refToThis.visitEnd();
                    }
                    isArrayVisited = true;
                }
            }
            return new ArraySignatureVisitor(Opcodes.ASM5);
        }

    }


    static final class ArrayRewriteSignatureVisitorTest extends SignatureWriter {

        private String arrayWrapperName = PathConstants.OBJECT_ARRAY_WRAPPER_NAME;

        public ArrayRewriteSignatureVisitorTest() {
            this(false);
        }

        private ArrayRewriteSignatureVisitorTest(boolean isClassBound) {
            super();
            this.isClassBoundAPI = isClassBound;
        }

        String lastClassType = null;


        boolean isClassBoundAPI = false;

        @Override
        public SignatureVisitor visitArrayType() {
            if (isClassBoundAPI) {
                return super.visitArrayType();
            }
            SignatureWriter refToThis = this;
            class ArraySignatureVisitor extends SignatureWriter {

                public ArraySignatureVisitor(int api) {
                    super();
                }

                private boolean isArrayVisited = false;


                @Override
                public void visitFormalTypeParameter(String name) {
                    super.visitFormalTypeParameter(name);
                }

                @Override
                public SignatureVisitor visitClassBound() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitClassBound();
                }

                @Override
                public SignatureVisitor visitInterfaceBound() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitInterfaceBound();
                }

                @Override
                public SignatureVisitor visitSuperclass() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitSuperclass();
                }

                @Override
                public SignatureVisitor visitInterface() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitInterface();
                }

                @Override
                public SignatureVisitor visitParameterType() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitParameterType();
                }

                @Override
                public SignatureVisitor visitReturnType() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitReturnType();
                }

                @Override
                public SignatureVisitor visitExceptionType() {
                    if (isArrayVisited) {
                        return new SignatureVisitor(Opcodes.ASM6) {
                        };
                    }
                    return super.visitExceptionType();
                }

                @Override
                public void visitBaseType(char c) {
                    if (!isArrayVisited) {
                        refToThis.visitClassType("xyz/acygn/millr/util/" + Type.getType(String.valueOf(c)).getClassName() + "ArrayWrapper");
                        refToThis.visitEnd();
                        return;
                    }
                    super.visitBaseType(c);
                    isArrayVisited = true;


                }

                @Override
                public SignatureVisitor visitArrayType() {
                    if (isClassBoundAPI) {
                        return super.visitArrayType();
                    }
                    if (!isArrayVisited) {
                        refToThis.visitClassType(arrayWrapperName);
                        refToThis.visitEnd();
                        isArrayVisited = true;
                    }
                    return new SignatureVisitor(Opcodes.ASM6) {
                    };

                }

                @Override
                public void visitClassType(String name) {
                    lastClassType = name;
                    if (!isArrayVisited) {
                        refToThis.visitClassType(arrayWrapperName);
                        refToThis.visitEnd();
                        isArrayVisited = true;
                    }
                    if (isClassBoundAPI) {
                        counter += 1;
                    }
                }

                @Override
                public void visitInnerClassType(String name) {
                    super.visitInnerClassType(name);
                }

                @Override
                public void visitTypeArgument() {
                    super.visitTypeArgument();
                }


                @Override
                public SignatureVisitor visitTypeArgument(char wildcard) {
                    return super.visitTypeArgument(wildcard);
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                }

                @Override
                public String toString() {
                    return super.toString();
                }

                @Override
                public void visitTypeVariable(String name) {
                    if (!isArrayVisited) {
                        refToThis.visitClassType(arrayWrapperName);
                        refToThis.visitEnd();
                    }
                    isArrayVisited = true;
                }
            }
            return new ArraySignatureVisitor(Opcodes.ASM5);
        }


        @Override
        public void visitClassType(String name) {
            lastClassType = name;
            if (isClassBoundAPI) {
                counter += 1;
            }
            super.visitClassType(name);
        }

        int counter;


        @Override
        public void visitTypeArgument() {
            if (TypeUtil.isNotProject(TypeUtil.getObjectType(lastClassType)) && isClassBoundAPI) {
                isClassBoundAPI = true;
                counter = 0;
            }
            super.visitTypeArgument();
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            if (TypeUtil.isNotProject(TypeUtil.getObjectType(lastClassType)) && isClassBoundAPI == false) {
                isClassBoundAPI = true;
                counter = 0;
            }
            return super.visitTypeArgument(wildcard);
        }
    }
}
