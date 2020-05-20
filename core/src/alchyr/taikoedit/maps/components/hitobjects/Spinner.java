package alchyr.taikoedit.maps.components.hitobjects;

import alchyr.taikoedit.maps.components.HitObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Spinner extends HitObject {
    public static Texture texture;

    private static final Color spinner = Color.LIGHT_GRAY.cpy();

    private int endPos;

    public Spinner(String[] params)
    {
        for (int i = 0; i < params.length; ++i) //to avoid out of bounds.
        {
            switch (i)
            {
                case 0:
                    x = Integer.parseInt(params[i]);
                    break;
                case 1:
                    y = Integer.parseInt(params[i]);
                    break;
                case 2:
                    pos = Integer.parseInt(params[i]);
                    break;
                case 3:
                    int objectType = Integer.parseInt(params[i]);

                    newCombo = (objectType & NEWCOMBO) > 0;
                    colorSkip = (objectType & COLORSKIP) >>> 4;
                    break;
                case 4:
                    int hitSound = Integer.parseInt(params[i]);
                    normal = (hitSound & NORMAL) > 0;
                    whistle = (hitSound & WHISTLE) > 0;
                    finish = (hitSound & FINISH) > 0;
                    clap = (hitSound & CLAP) > 0;
                    break;
                case 5:
                    duration = Integer.parseInt(params[i]);
                    endPos = pos + duration;
                    break;
                case 6:
                    //hitsamples
                    String[] samples = params[i].split(":");
                    hitSample = new int[samples.length];
                    for (int n = 0; n < samples.length; ++n)
                    {
                        hitSample[n] = Integer.parseInt(samples[n]);
                    }
            }
        }
    }

    @Override
    public void render(SpriteBatch sb, ShapeRenderer sr, int pos, float viewScale, float x, float y) {
        sb.setColor(spinner);
        sb.draw(texture, x + (this.endPos - pos) * viewScale - CIRCLE_OFFSET, y, CIRCLE_OFFSET, CIRCLE_OFFSET, CIRCLE_SIZE, CIRCLE_SIZE,
                LARGE_SCALE, LARGE_SCALE, 0, 0, 0, CIRCLE_SIZE, CIRCLE_SIZE, false, false);
        sb.draw(texture, x + (this.pos - pos) * viewScale - CIRCLE_OFFSET, y, CIRCLE_OFFSET, CIRCLE_OFFSET, CIRCLE_SIZE, CIRCLE_SIZE,
                LARGE_SCALE, LARGE_SCALE, 0, 0, 0, CIRCLE_SIZE, CIRCLE_SIZE, false, false);
    }
}
