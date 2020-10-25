package chord;

public class Mantainer implements Runnable{
    private Chord ring;
    private String type;

    public Mantainer(Chord ring, String type){
        this.ring = ring;
        this.type = type;
    }

    @Override
    public void run(){
        try {
            switch (type) {
                case "stabilize":
                    this.ring.stabilize();
                    break;
                case "check_predecessor":
                    this.ring.check_predecessor();
                    break;
                case "fix_fingers":
                    this.ring.fix_fingers();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}