package xyz.acygn.millr.testclasses;

/**
 * A sample class representing any arbitrary class supporting a number
 * of different simple operations, such as direct access to its fields,
 * pure methods, mutator methods, and so on.
 *
 * @author Marcello De Bernardi
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Rectangle {
    public double sideA;
    public double sideB;


    public Rectangle(Double sideA, Double sideB) {
        this.sideA = sideA;
        this.sideB = sideB;
    }


    public void setSideA(Double sideA) {
        this.sideA = sideA;
    }

    public void setSideB(Double sideB) {
        this.sideB = sideB;
    }

    public double getSideA(){
        return this.sideA;
    }

    public double getSideB(){
        return this.sideB;
    }

    public void scaleSize(int factor) {
        sideA *= factor;
        sideB *= factor;
    }

    public void scaleToMatchA(Rectangle rectangle) {
            Double scaleFactor = rectangle.sideA / this.sideA;
            this.sideA *= scaleFactor;
            this.sideB *= scaleFactor;
    }

    public void scaleToMatchB(Rectangle rectangle) {
            Double scaleFactor = rectangle.sideB / this.sideB;
            this.sideA *= scaleFactor;
            this.sideB *= scaleFactor;
    }

    public Double getArea() {
        return sideA * sideB;
    }

    public Double getPerimeter() {
        return (2 * sideA) + (2 * sideB);
    }

}
