import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DemoViewer {

    //Create a triangle list to form tetrahedron
    private List<Triangle> tris=new ArrayList<>();

    public DemoViewer(){
        //creating a new window
        JFrame frame=new JFrame();
        Container pane=frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pane.setLayout(new BorderLayout());

        //adding a slider to control horizontal rotation
        JSlider headingSlider=new JSlider(0,360,180);
        pane.add(headingSlider,BorderLayout.SOUTH);

        //slider to control vertical rotation
        JSlider pitchSlider=new JSlider(SwingConstants.VERTICAL,-90,90,0);
        pane.add(pitchSlider,BorderLayout.EAST);

        //panel to display render results
        JPanel renderPanel=new JPanel(){
            public void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        pane.add(renderPanel,BorderLayout.CENTER);

        frame.setSize(400,400);
        frame.setVisible(true);

        //create four triangles

    }
}
