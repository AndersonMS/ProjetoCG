package packageSpaceRunner;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bounding.BoundingVolume;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpaceRunner extends SimpleApplication implements AnalogListener {

    private BitmapFont defaultFont;
    private boolean START;
    private int difficulty, Score, colorInt, highCap, lowCap, diffHelp;
    private Node player;
    private Geometry CubeOld;
    private ArrayList<Geometry> cubeField;
    private ArrayList<ColorRGBA> obstacleColors;
    private float speed, coreTime, coreTime2, camAngle = 0;
    private BitmapText fpsScoreText, pressStart;
    private final float fpsRate = 1000f / 1f;

    public static void main(String[] args) {
        SpaceRunner app = new SpaceRunner();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        createLigth();

        Logger.getLogger("com.jme3").setLevel(Level.WARNING);

        flyCam.setEnabled(false);
        setDisplayStatView(false);

        Keys();

        defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        pressStart = new BitmapText(defaultFont, false);
        fpsScoreText = new BitmapText(defaultFont, false);

        loadText(fpsScoreText, "Pontos atuais: 0", defaultFont, 0, 2, 0);
        loadText(pressStart, "Aperte ENTER", defaultFont, 0, 5, 0);

        player = createPlayer();
        rootNode.attachChild(player);
        cubeField = new ArrayList<>();
        obstacleColors = new ArrayList<>();

        gameReset();
    }

    private void gameReset() {
        Score = 0;
        lowCap = 10;
        colorInt = 0;
        highCap = 40;
        difficulty = highCap;

        for (Geometry cube : cubeField) {
            cube.removeFromParent();
        }
        cubeField.clear();

        if (CubeOld != null) {
            CubeOld.removeFromParent();
        }
        CubeOld = createFirstCube();

        obstacleColors.clear();
        obstacleColors.add(ColorRGBA.Orange);
        obstacleColors.add(ColorRGBA.Red);
        obstacleColors.add(ColorRGBA.Yellow);
        renderer.setBackgroundColor(ColorRGBA.White);
        speed = lowCap / 400f;
        coreTime = 20.0f;
        coreTime2 = 10.0f;
        diffHelp = lowCap;
    }

    @Override
    public void simpleUpdate(float tpf) {
        camTakeOver(tpf);
        if (START) {
            gameLogic(tpf);
        }
    }

    private void camTakeOver(float tpf) {
        cam.setLocation(player.getLocalTranslation().add(-8, 2, 0));
        cam.lookAt(player.getLocalTranslation(), Vector3f.UNIT_Y);

        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(camAngle, Vector3f.UNIT_Z);
        cam.setRotation(cam.getRotation().mult(rot));
        camAngle *= FastMath.pow(.99f, fpsRate * tpf);
    }

    @Override
    public void requestClose(boolean esc) {
        if (!esc) {
            System.out.println("Desistiu do jogo.");
        } else {
            System.out.println("O jogador matou um OTO. O total de pontos final foi: " + Score);
        }
        context.destroy(false);
    }

    private void randomCube() {
        Geometry cube = CubeOld.clone();
        int playerX = (int) player.getLocalTranslation().getX();
        int playerZ = (int) player.getLocalTranslation().getZ();
        float x = FastMath.nextRandomInt(playerX + difficulty + 30, playerX + difficulty + 90);
        float z = FastMath.nextRandomInt(playerZ - difficulty - 50, playerZ + difficulty + 50);
        cube.getLocalTranslation().set(x, 0, z);

        rootNode.attachChild(cube);
        cubeField.add(cube);
    }

    private Geometry createFirstCube() {
        Vector3f loc = player.getLocalTranslation();
        loc.addLocal(4, 0, 0);
        Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);
        geom.setLocalTranslation(loc);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture(new TextureKey("Models/Images/Teste.png", false))); // with Unshaded.j3md

        geom.setMaterial(mat);

        return geom;
    }

    private Node createPlayer() {

        Node playerCar = (Node) assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");

        playerCar.setLocalTranslation(0, 0, 0);
        playerCar.scale(0.35f, 0.35f, 0.35f);
        playerCar.rotate(0, 1.56f, 0);

        return playerCar;
    }

    private void gameLost() {
        START = false;
        loadText(pressStart, "Você perdeu! Aperte ENTER para jogar de novo.", defaultFont, 0, 5, 0);
        gameReset();
    }

    private void gameLogic(float tpf) {
        if (timer.getTimeInSeconds() >= coreTime2) {
            coreTime2 = timer.getTimeInSeconds() + 10;
            if (difficulty <= lowCap) {
                difficulty = lowCap;
            } else if (difficulty > lowCap) {
                difficulty -= 5;
                diffHelp += 1;
            }
        }

        if (speed < .1f) {
            speed += .000001f * tpf * fpsRate;
        }

        player.move(speed * tpf * fpsRate, 0, 0);
        if (cubeField.size() > difficulty) {
            cubeField.remove(0);
        } else if (cubeField.size() != difficulty) {
            randomCube();
        }

        if (cubeField.isEmpty()) {
            requestClose(false);
        } else {
            for (int i = 0; i < cubeField.size(); i++) {

                Geometry playerModel = (Geometry) player.getChild(0);
                Geometry cubeModel = cubeField.get(i);

                BoundingVolume pVol = playerModel.getWorldBound();
                BoundingVolume vVol = cubeModel.getWorldBound();

                if (pVol.intersects(vVol)) {
                    gameLost();
                    return;
                }
                if (cubeField.get(i).getLocalTranslation().getX() + 10 < player.getLocalTranslation().getX()) {
                    cubeField.get(i).removeFromParent();
                    cubeField.remove(cubeField.get(i));
                }

            }
        }

        Score += fpsRate * tpf;
        fpsScoreText.setText("Pontos atuais: " + Score);
    }

    private void Keys() {
        inputManager.addMapping("START", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(this, "START", "Left", "Right");
    }

    public void onAnalog(String binding, float value, float tpf) {
        if (binding.equals("START") && !START) {
            START = true;
            guiNode.detachChild(pressStart);
            System.out.println("START");
        } else if (START == true && binding.equals("Left")) {
            player.move(0, 0, -(speed / 2f) * value * fpsRate);
            camAngle -= value * tpf;
        } else if (START == true && binding.equals("Right")) {
            player.move(0, 0, (speed / 2f) * value * fpsRate);
            camAngle += value * tpf;
        }
    }

    private void loadText(BitmapText txt, String text, BitmapFont font, float x, float y, float z) {
        txt.setSize(font.getCharSet().getRenderedSize());
        txt.setLocalTranslation(txt.getLineWidth() * x, txt.getLineHeight() * y, z);
        txt.setText(text);
        guiNode.attachChild(txt);
    }

    private void createLigth() {

        DirectionalLight l1 = new DirectionalLight();
        l1.setDirection(new Vector3f(1, -0.7f, 0));
        rootNode.addLight(l1);

        DirectionalLight l2 = new DirectionalLight();
        l2.setDirection(new Vector3f(-1, 0, 0));
        rootNode.addLight(l2);

        DirectionalLight l3 = new DirectionalLight();
        l3.setDirection(new Vector3f(0, 0, -1.0f));
        rootNode.addLight(l3);

        DirectionalLight l4 = new DirectionalLight();
        l4.setDirection(new Vector3f(0, 0, 1.0f));
        rootNode.addLight(l4);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White);
        rootNode.addLight(ambient);
    }
}
