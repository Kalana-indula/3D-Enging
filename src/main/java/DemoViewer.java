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
        final List<Triangle> tris = new ArrayList<>();

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

        //call inflate here to subdivide the triangles
        List<Triangle> inflatedTris=inflate(tris);

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

//                for (Triangle t : inflatedTris) {
//                    // Transform each vertex using the rotation matrix
//                    Vertex v1 = transform.transform(t.v1);
//                    Vertex v2 = transform.transform(t.v2);
//                    Vertex v3 = transform.transform(t.v3);
//
//                    // Translate to the center of the screen
//                    v1.x += getWidth() / 2;
//                    v1.y += getHeight() / 2;
//                    v2.x += getWidth() / 2;
//                    v2.y += getHeight() / 2;
//                    v3.x += getWidth() / 2;
//                    v3.y += getHeight() / 2;
//
//
//                }

                //create a bufferedImage to draw the triangle directly into a pixel buffer
                BufferedImage img=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);

                //Create a z-buffer array to store depth values for each pixel in the image
                //The z-buffer ensures that only the closest (visible) objects are rendered
                double[] zBuffer=new double[img.getWidth()*img.getHeight()];

                //Initialize the z-buffer with extremely far away depths
                //This ensures that any rendered object will be closer than the initial value
                for(int q=0; q < zBuffer.length; q++){
                    zBuffer[q]=Double.NEGATIVE_INFINITY;
                }

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

                    // Draw the wireframe using the transformed vertices
                    Path2D path = new Path2D.Double();
                    path.moveTo(v1.x, v1.y);
                    path.lineTo(v2.x, v2.y);
                    path.lineTo(v3.x, v3.y);
                    path.closePath();
                    g2.draw(path);

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
                                //Calculate the depth of the current pixel in using barycentric interpolation
                                //This gives the z-coordinate of the pixel in 3D space
                                double depth=b1*v1.z+b2*v2.z+b3*v3.z;

                                //calculate the index of the current pixel in the z-buffer array
                                //The z-buffer is a 1D array representing a 2D image, so we use:y* width+x
                                int zIndex=y*img.getWidth()+x;

                                //Check if the current pixel is closer than the previously stored depth
                                if(zBuffer[zIndex]<depth){
                                    //set the pixel color to the triangle's color
                                    img.setRGB(x,y,t.color.getRGB());

                                    //update the z-buffer with the new depth
                                    zBuffer[zIndex]=depth;
                                }

                            }
                        }
                    }

                    //Calculate two edge vectors of the triangle
                    //ab=vector from v1 to v2
                    //ac=vector from v1 to v3
                    Vertex ab=new Vertex(v2.x-v1.x,v2.y-v1.y,v2.z-v1.z);
                    Vertex ac=new Vertex(v3.x-v1.x,v3.y-v1.y,v3.z-v1.z);

                    //Calculate the normal vector of the triaangle using the cross product of ab and ac
                    //The cross product of two vectors gives a vector perpedicular to both
                    Vertex norm=new Vertex(
                            ab.y*ac.z-ab.z*ac.y,
                            ab.z*ac.x-ab.x*ac.z,
                            ab.x*ac.y-ab.y*ac.x
                    );

                    //Calculate the length(magnitude) of the normal vector
                    //This is used to normalize the vector (make it a unit vector)
                    double normalLength=Math.sqrt(norm.x*norm.x+norm.y*norm.y+norm.z*norm.z);

                    //Normalize the normal vector by dividing each component by its length
                    //A unit normal vector has length of 1 and is used for lighting calculations
                    norm.x/=normalLength;
                    norm.y/=normalLength;
                    norm.z/=normalLength;

                    //calculate cosine between triangle normal and light direction
                    double angleCos=Math.abs(norm.z);

                    //apply shading
                    Color shadedColor=getShade(t.color,angleCos);

                    //set color for rendering
                    g2.setColor(shadedColor);
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

    public static Color getShade(Color color,double shade){
       //apply gamma correction to the color channels (red,green,blue)
        //gamma correction is used to adjust the intensity of the color

        double redLinear=Math.pow(color.getRed(),2.4)*shade;
        double greenLinear=Math.pow(color.getGreen(),2.4)*shade;
        double blueLinear=Math.pow(color.getBlue(),2.4)*shade;

        //apply inverse gamma correction
        int red=(int) Math.pow(redLinear,1/2.4);
        int green=(int) Math.pow(greenLinear,1/2.4);
        int blue=(int) Math.pow(blueLinear,1/2.4);

        //return a new color object with clamped RGB values
//        return new Color(Math.min(255,red),Math.min(255,green),Math.min(255,blue));
        return new Color(red,green,blue);
    }

    //create sphere
    public static List<Triangle> inflate(List<Triangle> tris){
        List<Triangle> result=new ArrayList<>();

        for(Triangle t:tris){
            Vertex m1=new Vertex((t.v1.x+t.v2.x)/2,(t.v1.y+t.v2.y)/2,(t.v1.z+t.v2.z)/2);
            Vertex m2=new Vertex((t.v2.x+t.v3.x)/2,(t.v2.y+t.v3.y)/2,(t.v2.z+t.v3.z)/2);
            Vertex m3=new Vertex((t.v1.x+t.v3.x)/2,(t.v1.y+t.v3.y)/2,(t.v1.z+t.v3.z)/2);

            result.add(new Triangle(t.v1,m1,m3,t.color));
            result.add(new Triangle(t.v2,m1,m2,t.color));
            result.add(new Triangle(t.v3,m2,m3,t.color));
            result.add(new Triangle(m1,m2,m3,t.color));
        }

        for(Triangle t:result){
            for(Vertex v:new Vertex[]{t.v1,t.v2,t.v3}){
                double l=Math.sqrt(v.x*v.x+v.y*v.y+v.z*v.z)/Math.sqrt(30000);
                v.x/=l;
                v.y/=l;
                v.z/=l;
            }
        }

        return result;
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