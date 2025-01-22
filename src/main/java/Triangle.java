import java.awt.*;

public class Triangle {
    private Vertex v1;
    private Vertex v2;
    private Vertex v3;
    private Color color;

    //create a triagle as the instance created
    Triangle(Vertex v1,Vertex v2,Vertex v3,Color color){
        this.v1=v1;
        this.v2=v2;
        this.v3=v3;
        this.color=color;
    }
}
