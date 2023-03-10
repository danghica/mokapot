package xyz.acygn.millr;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.Optional;

class FieldParameter {

    int access;
    String name;
    String desc;
    String signature;
    ClassParameter cp;

    public FieldParameter(ClassParameter cp, int access, String name, String desc, String signature) {
        this.cp = cp;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof FieldParameter)) {
            return false;
        }
        FieldParameter field = (FieldParameter) o;
        return field.cp.equals(cp) && areEqual(field.name, this.name)
                & areEqual(field.desc, this.desc);
    }

    @Override
    public int hashCode() {
        return getHash(name) | getHash(desc);
    }

    public int getHash(String str) {
        return (str == null) ? 0 : str.hashCode();
    }

    public boolean areEqual(String strOne, String strTwo) {
        return (strOne == null) ? strTwo == null : strOne.equals(strTwo);
    }

    @Override
    public String toString() {
        return cp.toString() + "\n" + "name: " + name + ", desc: " + desc;
    }

    static FieldParameter getFieldParameter(String owner, String nameField, String desc) {
        Optional<ClassDataBase.ClassData> ocd = ClassDataBase.getClassDataOrNull(owner);
        if (ocd.isPresent()) {
            Optional<FieldParameter> ofp =
                    ocd.get().getDirectFields().stream().filter(e -> e.name.equals(nameField) && e.desc.equals(desc)).findAny();
            if (ofp.isPresent()) {
                return ofp.get();
            }
        }
        ClassReader cr = Mill.getInstance().getClassReader(owner);
        ClassParameter cp = ClassParameter.getClassParameter(cr);
        ClassNode cn = new ClassNode(Opcodes.ASM6);
        cr.accept(cn, ClassReader.SKIP_CODE);
        Optional<FieldNode> ofn = cn.fields.stream().filter(e -> e.name.equals(nameField) && e.desc.equals(desc)).findAny();
        if (!ofn.isPresent()) {
            for (String interfaces : cp.classInterfaces) {
                try {
                    return getFieldParameter(interfaces, nameField, desc);
                } catch (RuntimeException ex) {

                }
            }
            if (cp.classSuperName != null) {
                try {
                    return getFieldParameter(cp.classSuperName, nameField, desc);
                } catch (RuntimeException ex) {

                }
            }
            throw new RuntimeException("field not found \n"
                    + "field owner : " + owner + "\n"
                    + "field name : " + nameField + "\n"
                    + "field desc :" + desc);
        }
        FieldNode fn = ofn.get();
        return new FieldParameter(cp, fn.access, fn.name, fn.desc, fn.signature);
    }


}