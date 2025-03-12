import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class DemoViewer {

    public DemoViewer() {
        //creating a new window
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pane.setLayout(new BorderLayout());

        //adding a slider to control horizontal rotation
        JSlider headingSlider = new JSlider(0, 360, 180);
        pane.add(headingSlider, BorderLayout.SOUTH);

        //slider to control vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        //creating a triangle collection
        List<Triangle> tris = new ArrayList<>();

        tris.add(new Triangle(
                new Vertex(100, 100, 100),
                new Vertex(-100, -100, 100),
                new Vertex(-100, 100, -100),
                Color.WHITE));

        tris.add(new Triangle(
                new Vertex(100, 100, 100),
                new Vertex(-100, -100, 100),
                new Vertex(100, -100, -100),
                Color.RED));

        tris.add(new Triangle(
                new Vertex(-100, 100, -100),
                new Vertex(100, -100, -100),
                new Vertex(100, 100, 100),
                Color.GREEN));

        tris.add(new Triangle(
                new Vertex(-100, 100, -100),
                new Vertex(100, -100, -100),
                new Vertex(-100, -100, 100),
                Color.BLUE));


        //panel to display render results
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                //creating rotation matrix
                double heading = Math.toRadians(headingSlider.getValue());


                Matrix3 headingTransfrom = new Matrix3(new double[]{
                        Math.cos(heading), 0, Math.sin(heading),
                        0, 1, 0,
                        -Math.sin(heading), 0, Math.cos(heading)
                });

                double pitch = Math.toRadians(pitchSlider.getValue());

                Matrix3 pitchTransform = new Matrix3(new double[]{
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch)
                });

                Matrix3 transform = headingTransfrom.multiply(pitchTransform);

//                g2.translate(getWidth() / 2, getHeight() / 2);
                g2.setColor(Color.WHITE);

                for (Triangle t : tris) {
                    // Transform each vertex using the rotation matrix
                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);

                    // Translate to the center of the screen
                    v1.x += getWidth() / 2;
                    v1.y += getHeight() / 2;
                    v2.x += getWidth() / 2;
                    v2.y += getHeight() / 2;
                    v3.x += getWidth() / 2;
                    v3.y += getHeight() / 2;

                    // Draw the wireframe using the transformed vertices
                    Path2D path = new Path2D.Double();
                    path.moveTo(v1.x, v1.y);
                    path.lineTo(v2.x, v2.y);
                    path.lineTo(v3.x, v3.y);
                    path.closePath();
                    g2.draw(path);
                }


                //create a bufferedImage to draw the triangle directly into a pixel buffer
                BufferedImage img=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);

                //Iterate through each triangle in the list
                for(Triangle t:tris){
                    //Transform the triangle's vertices using the rotation matrix
                    Vertex v1=transform.transform(t.v1);
                    Vertex v2=transform.transform(t.v2);
                    Vertex v3=transform.transform(t.v3);

                    //Manually translate the vertices to the center of the screen
                    //(Since we're not using Graphic2D's translate method
                    v1.x+=getWidth()/2;
                    v1.y+=getHeight()/2;
                    v2.x+=getWidth()/2;
                    v2.y+=getHeight()/2;
                    v3.x+=getWidth()/2;
                    v3.y+=getHeight()/2;

                    //Calculate the rectangular bounding box of the triangle
                    //to limit the pixel iteration to the area where the triangle could be
                    int minX=(int) Math.max(0,Math.ceil(Math.min(v1.x,Math.min(v2.x,v3.x))));
                    int maxX=(int) Math.min(img.getWidth()-1,Math.floor(Math.max(v1.x,Math.max(v2.x,v3.x))));
                    int minY=(int) Math.max(0,Math.ceil(Math.min(v1.y,Math.min(v2.y,v3.y))));
                    int maxY=(int) Math.min(img.getHeight()-1,Math.floor(Math.max(v1.y,Math.max(v2.y,v3.y))));

                    //calculate the area of the triangle using the cross product
                    //This is used for barycentric coordinate calculations
                    double triangleArea=(v1.y-v3.y)*(v2.x-v3.x)+(v2.y-v3.y)*(v3.x-v1.x);

                    //Iterate over each pixel within the bounding box
                    for(int y=minY;y<=maxY;y++){
                        for(int x=minX;x<=maxX;x++){
                            //calculate barycentric coordinates (b1,b2,b3) for the current pixel(x,y)
                            //Barycentric coordinates determine if the pixel lies inside the triangle
                            double b1=((y-v3.y)*(v2.x-v3.x)+(v2.y-v3.y)*(v3.x-x))/triangleArea;
                            double b2=((y-v1.y)*(v3.x-v1.x)+(v3.y-v1.y)*(v1.x-x))/triangleArea;
                            double b3=((y-v2.y)*(v1.x-v2.x)+(v1.y-v2.y)*(v2.x-x))/triangleArea;

                            //check if the pixel lies inside the triangle
                            //Barycentric coordinates must be between 0 and 1
                            if(b1 >= 0 && b1 <= 1 && b2>= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1){
                                //set the pixel color to the triangle's color
                                img.setRGB(x,y,t.color.getRGB());
                            }
                        }
                    }
                }
                //Draw the final rendered image onto the Graphics2D object
                g2.drawImage(img,0,0,null);
            }
        };

        pane.add(renderPanel, BorderLayout.CENTER);

        frame.setSize(400, 400);
        frame.setVisible(true);

        //adding listeners
        headingSlider.addChangeListener(e -> renderPanel.repaint());
        pitchSlider.addChangeListener(e -> renderPanel.repaint());

    }
}

//creating vertexes
class Vertex {
    //create position of vertex
    double x;
    double y;
    double z;

    Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

//creating a triangle
class Triangle {
    Vertex v1;
    Vertex v2;
    Vertex v3;
    Color color;

    //create a triangle as the instance created
    Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
    }
}

//handle matrix calculations
class Matrix3 {
    double[] values;

    Matrix3(double[] values) {
        this.values = values;
    }

    Matrix3 multiply(Matrix3 other) {
        double[] result = new double[9];

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                for (int i = 0; i < 3; i++) {
                    result[row * 3 + col] += this.values[row * 3 + i] * other.values[i * 3 + col];
                }
            }
        }
        return new Matrix3(result);
    }

    Vertex transform(Vertex in) {
        return new Vertex(
                in.x * values[0] + in.y * values[3] + in.z * values[6],
                in.x * values[1] + in.y * values[4] + in.z * values[7],  // corrected line
                in.x * values[2] + in.y * values[5] + in.z * values[8]   // corrected line
        );
    }

}