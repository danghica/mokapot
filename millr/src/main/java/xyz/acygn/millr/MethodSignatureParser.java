


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

/**
 * author = thomas
 */

public class MethodSignatureParser {


    static public class MethodSignatureParserVisitor extends SignatureVisitor {

        public MethodSignatureParserVisitor(int api) {
            super(api);
        }

        List<SignatureWriter> listParametersWriter = new ArrayList<>();
        List<String> listParameters = new ArrayList<>();

        class registerParameterType extends SignatureWriter {
            @Override
            public void visitEnd() {
                super.visitEnd();
                listParameters.add(this.toString());
            }
        }

        String returnType;

        class registerReturnType extends SignatureWriter {
            @Override
            public void visitEnd() {
                super.visitEnd();
                returnType = this.toString();
            }
        }

        @Override
        public SignatureVisitor visitParameterType() {
            SignatureWriter sw = new SignatureWriter();
            listParametersWriter.add(sw);
            return sw;
        }

        SignatureWriter returnTypeWriter = new SignatureWriter();

        @Override
        public SignatureVisitor visitReturnType() {
            return returnTypeWriter;
        }

        List<SignatureWriter> listExceptionWriter = new ArrayList<>();
        List<String> ExceptionTypes = new ArrayList<>();

        class RegisterExceptionType extends SignatureWriter {
            @Override
            public void visitEnd() {
                super.visitEnd();
                ExceptionTypes.add(this.toString());
            }
        }

        @Override
        public SignatureVisitor visitExceptionType() {
            SignatureWriter sw = new SignatureWriter();
            listExceptionWriter.add(sw);
            return sw;
        }


        private void resolveAllString() {
            listParameters = (listParametersWriter.stream().map(e -> e.toString()).collect(Collectors.toList()));
            ExceptionTypes = (listExceptionWriter.stream().map(e -> e.toString()).collect(Collectors.toList()));
            returnType = returnTypeWriter.toString();
        }

        public String[] getAllParameters() {
            resolveAllString();
            List<String> allParam = new ArrayList<>();
            allParam.addAll(listParameters);
            allParam.add(returnType);
            allParam.addAll(ExceptionTypes);
            return allParam.toArray(new String[0]);

        }

        public String[] getArguements() {
            resolveAllString();
            return (String[]) listParameters.toArray(new String[0]);
        }

        public String getReturn() {
            resolveAllString();
            return returnType;
        }

        public String[] getExceptionType() {
            resolveAllString();
            return (String[]) ExceptionTypes.toArray(new String[0]);
        }
    }

    public static String[] getArguements(String signature) {
        SignatureReader sr = new SignatureReader(signature);
        MethodSignatureParserVisitor sv = new MethodSignatureParserVisitor(Opcodes.ASM5);
        sr.accept(sv);
        return sv.getArguements();
    }

    public static String[] getExceptions(String signature) {
        SignatureReader sr = new SignatureReader(signature);
        MethodSignatureParserVisitor sv = new MethodSignatureParserVisitor(Opcodes.ASM5);
        sr.accept(sv);
        return sv.getExceptionType();
    }


    public static String getReturn(String signature) {
        SignatureReader sr = new SignatureReader(signature);
        MethodSignatureParserVisitor sv = new MethodSignatureParserVisitor(Opcodes.ASM5);
        sr.accept(sv);
        return sv.getReturn();
    }

    public static String[] getAllParam(String signature) {
        SignatureReader sr = new SignatureReader(signature);
        MethodSignatureParserVisitor sv = new MethodSignatureParserVisitor(Opcodes.ASM5);
        sr.accept(sv);
        return sv.getAllParameters();
    }

    /**
     * Only work if the signature is the signature of a method
     * @param signature
     */
    public static String getTypeParameterPart(String signature){
        class oneOfStringStorage{
            String storage = null;
            void updateString(String stringUpdate){
                if (storage == null){
                    storage = stringUpdate;
                }
            }
        }
        final oneOfStringStorage oneSSS = new oneOfStringStorage();
        SignatureReader sr = new SignatureReader(signature);
        sr.accept(new SignatureWriter(){

            @Override
            public SignatureVisitor visitParameterType() {
                oneSSS.updateString(this.toString());
                return super.visitParameterType();
            }

            @Override
            public SignatureVisitor visitReturnType() {
                oneSSS.updateString(this.toString());
                return super.visitReturnType();
            }
        });
        return (oneSSS.storage==null || oneSSS.storage.length()==0)? "" : oneSSS.storage +">";

    }



}
