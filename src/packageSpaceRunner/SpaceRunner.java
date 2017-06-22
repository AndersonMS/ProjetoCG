package packageSpaceRunner;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioNode;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class SpaceRunner extends SimpleApplication implements AnalogListener {

    private BitmapFont defaultFont;
    private boolean START, setDificuldade = false;
    private int difficulty, Score, colorInt, highCap, lowCap, diffHelp;
    private Node player;
    private Geometry CubeOld;
    private ArrayList<Geometry> cubeField;
    private ArrayList<ColorRGBA> obstacleColors;
    private float speed, coreTime, coreTime2, camAngle = 0;
    private BitmapText fpsScoreText, pressStart, nameGame, scoreText;
    private final float fpsRate = 1000f / 1f;
    private static String nome;
    private SortedMap<Integer, String> map = new TreeMap<Integer, String>(Collections.reverseOrder());
    private AudioNode audio;

    public static void main(String[] args) {
        SpaceRunner app = new SpaceRunner();
        app.start();
        app.setDisplayFps(false);
    }

    @Override
    public void simpleInitApp() {
        this.audio = new AudioNode(assetManager, "Audio/AudioJogo.ogg", false);
        
        createLigth();

        Logger.getLogger("com.jme3").setLevel(Level.WARNING);

        flyCam.setEnabled(false);
        setDisplayStatView(false);

        Keys();

        defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        pressStart = new BitmapText(defaultFont, false);
        fpsScoreText = new BitmapText(defaultFont, false);
        nameGame = new BitmapText(defaultFont, false);
        scoreText = new BitmapText(defaultFont, false);

        loadText(fpsScoreText, "Pontos atuais: 0", defaultFont, 0, 50, 0);
        guiNode.detachChild(fpsScoreText);
        createMenu();

        player = createPlayer();
        rootNode.attachChild(player);
        rootNode.attachChild(audio);
        cubeField = new ArrayList<>();
        obstacleColors = new ArrayList<>();
        audio.setLooping(true);
        audio.setVolume(0.5f);
        audio.setPositional(false);
        audio.play();
    }

    private void gameReset() {
        Score = 0;
        colorInt = 0;
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
        map.put(Score, nome);
        guiNode.detachChild(fpsScoreText);
        loadText(scoreText, nome + " sua pontuacao foi " + Score + "!\n\n"
                + "Menu Principal (Pressione X)", defaultFont, 200, 360, 0);
        loadText(nameGame, "Space Runner", defaultFont, 200, 485, 0, 40);
        START = false;
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

        Score += 1;
        fpsScoreText.setText("Pontos atuais: " + Score);
    }

    private void Keys() {
        inputManager.addMapping("START", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("ESPACE", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reiniciar", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("Ajuda", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Voltar", new KeyTrigger(KeyInput.KEY_V));
        inputManager.addMapping("Sobre", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Opcoes", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("Som", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Dificuldade", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Ligado", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("Desligado", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Facil", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Medio", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Dificil", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("StartGame", new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping("MenuPrincipal", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("Melhores", new KeyTrigger(KeyInput.KEY_M));
        inputManager.addListener(this, "START", "Left", "Right", "ESPACE", "Reiniciar", "Ajuda", "Voltar", "Sobre", "Opcoes",
                "Som", "Dificuldade", "Ligado", "Desligado", "Facil", "Medio", "Dificil", "StartGame", "MenuPrincipal", "Melhores");
    }

    @Override
    public void onAnalog(String binding, float value, float tpf) {
        if (binding.equals("START") && !START) {
            START = true;
            guiNode.detachChild(pressStart);
            guiNode.detachChild(scoreText);
            guiNode.attachChild(fpsScoreText);
        } else if (START && binding.equals("Left")) {
            player.move(0, 0, -(speed / 2f) * value * fpsRate);
            camAngle -= value * tpf;
        } else if (START && binding.equals("Right")) {
            player.move(0, 0, (speed / 2f) * value * fpsRate);
            camAngle += value * tpf;
        } else if (START && binding.equals("ESPACE")) {
            START = false;
            loadText(pressStart, "Voltar ao jogo (Pressione ENTER)\nReiniciar (Pressione R)\nSair (Pressione ESC)", defaultFont, 200, 170, 0);
        } else if (!START && binding.equals("Reiniciar")) {
            START = true;
            guiNode.detachChild(pressStart);
            gameReset();
        } else if (!START && binding.equals("Ajuda")) {
            loadText(pressStart, "Utilize as teclas de direcao abaixo do DELETE para movimentar a nave.\n"
                    + "Durante o jogo pressione ESPACE para pausar.\n"
                    + "Voltar (Pressione V)", defaultFont, 80, 170, 0);
        } else if (!START && binding.equals("Voltar")) {
            createMenu();
        } else if (!START && binding.equals("Sobre")) {
            loadText(pressStart, "Desenvolvido por: Joao Marcos Figueira (141018)\n"
                    + "\t\t        Anderson Martins da Silva (140513)\n"
                    + "Git: https://github.com/AndersonMS/ProjetoCG\n"
                    + "Voltar (Pressione V)", defaultFont, 120, 170, 0);
        } else if (!START && binding.equals("Opcoes")) {
            loadText(pressStart, "Som (Pressione F)\n"
                    + "Dificuldade (Pressione D)\n"
                    + "Voltar (Pressione V)", defaultFont, 230, 170, 0);
        } else if (!START && binding.equals("Som")) {
            loadText(pressStart, "Ligado (Pressione L)\n"
                    + "Desligado (Pressione K)", defaultFont, 230, 170, 0);
        } else if (!START && binding.equals("Dificuldade")) {
            loadText(pressStart, "Facil (Pressione 1)\n"
                    + "Medio (Pressione 2)\n"
                    + "Dificil (Pressione 3)", defaultFont, 250, 170, 0);
        } else if (!START && binding.equals("Ligado")) {
            audio.setVolume(0.5f);
            loadText(pressStart, "Som (Pressione F)\n"
                    + "Dificuldade (Pressione D)\n"
                    + "Voltar (Pressione V)", defaultFont, 230, 170, 0);
        } else if (!START && binding.equals("Desligado")) {
            audio.setVolume(0f);
            loadText(pressStart, "Som (Pressione F)\n"
                    + "Dificuldade (Pressione D)\n"
                    + "Voltar (Pressione V)", defaultFont, 230, 170, 0);
        } else if (!START && binding.equals("Facil")) {
            setDificuldade = true;
            highCap = 30;
            lowCap = 10;
            loadText(pressStart, "Som (Pressione F)\n"
                    + "Dificuldade (Pressione D)\n"
                    + "Voltar (Pressione V)", defaultFont, 230, 170, 0);
        } else if (!START && binding.equals("Medio")) {
            setDificuldade = true;
            highCap = 50;
            lowCap = 20;
            loadText(pressStart, "Som (Pressione F)\n"
                    + "Dificuldade (Pressione D)\n"
                    + "Voltar (Pressione V)", defaultFont, 230, 170, 0);
        } else if (!START && binding.equals("Dificil")) {
            setDificuldade = true;
            highCap = 50;
            lowCap = 30;
            loadText(pressStart, "Som (Pressione F)\n"
                    + "Dificuldade (Pressione D)\n"
                    + "Voltar (Pressione V)", defaultFont, 230, 170, 0);
        } else if (!START && binding.equals("MenuPrincipal")) {
            guiNode.detachChild(scoreText);
            guiNode.detachChild(fpsScoreText);
            gameReset();
            createMenu();
        } else if (!START && binding.equals("StartGame")) {
            try {
                Thread.sleep((long) 500);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpaceRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
            nome = JOptionPane.showInputDialog("Qual o seu nome?");
            START = true;
            guiNode.detachChild(nameGame);
            guiNode.detachChild(pressStart);
            guiNode.attachChild(fpsScoreText);
            if (!setDificuldade) {
                highCap = 30;
                lowCap = 10;
            }
            gameReset();
        } else if (!START && binding.equals("Melhores")) {
            Iterator iterator = map.entrySet().iterator();
            Integer o = 0;
            String b = "\tMelhores\n\n";
            while(iterator.hasNext()){
                if(o++ == 3)
                    break;
                Map.Entry entry = (Map.Entry) iterator.next();
                b += entry.getValue() + ": " + entry.getKey() + "\n";
            }
            loadText(pressStart, b + "Voltar (Pressione V)", defaultFont, 230, 170, 0);
        }
    }

    private void loadText(BitmapText txt, String text, BitmapFont font, float x, float y, float z) {
        txt.setSize(font.getCharSet().getRenderedSize());
        txt.setLocalTranslation(0, y, z);
        txt.setText(text);
        guiNode.attachChild(txt);
    }

    private void loadText(BitmapText txt, String text, BitmapFont font, float x, float y, float z, float tam) {
        txt.setSize(tam);
        txt.setLocalTranslation(0, y, z);
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

    private void createMenu() {
        loadText(nameGame, "Space Runner", defaultFont, 0, 400, 0, 40);
        loadText(pressStart, "Novo Jogo (Pressione B)\n"
                + "Melhores (Pressione M)\n"
                + "Opcoes (Pressione O)\n"
                + "Ajuda (Pressione A)\n"
                + "Sobre (Pressione S)\n"
                + "Sair (Pressione ESC)", defaultFont, 0, 170, 0);
}    }

