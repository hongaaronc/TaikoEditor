package alchyr.taikoedit.util.assets;

import alchyr.taikoedit.management.AssetMaster;
import alchyr.taikoedit.util.TrueTypeFontGenerator;
import alchyr.taikoedit.util.structures.Pair;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static alchyr.taikoedit.TaikoEditor.assetMaster;
import static alchyr.taikoedit.TaikoEditor.editorLogger;

public class AssetLists implements Json.Serializable {
    private final HashMap<String, ArrayList<AssetInfo>> lists = new HashMap<>();
    private final HashSet<String> loadedLists = new HashSet<>();

    public static final TextureLoader.TextureParameter mipmaps;
    public static final TextureLoader.TextureParameter linear;

    static {
        mipmaps = new TextureLoader.TextureParameter();
        mipmaps.genMipMaps = true;
        mipmaps.magFilter = Texture.TextureFilter.MipMapLinearNearest;
        mipmaps.minFilter = Texture.TextureFilter.MipMapLinearNearest;

        linear = new TextureLoader.TextureParameter();
        linear.magFilter = Texture.TextureFilter.Linear;
        linear.minFilter = Texture.TextureFilter.Linear;
    }

    public void loadList(String name, AssetManager manager)
    {
        editorLogger.info("Loading asset list \"" + name + "\"");

        if (lists.containsKey(name))
        {
            if (!loadedLists.contains(name))
            {
                String[] params;

                for (AssetInfo info : lists.get(name))
                {
                    try
                    {
                        switch (info.getType())
                        {
                            case "texture":
                                manager.load(info.getFileName(), Texture.class, linear);
                                assetMaster.loadedAssets.put(info.getAssetName(name), info.getFileName());
                                break;
                            case "largetexture":
                                manager.load(info.getFileName(), Texture.class, mipmaps);
                                assetMaster.loadedAssets.put(info.getAssetName(name), info.getFileName());
                                break;
                            case "region":
                                if (!manager.contains(info.getFileName()))
                                    manager.load(info.getFileName(), Texture.class, mipmaps); //Assume atlases have large images

                                params = info.getParams().split(" ");
                                assetMaster.loadingRegions.add(new Pair<>(info.getAssetName(name), new RegionInfo(info.getFileName(), params)));
                                break;
                            case "bitmapfont":
                                manager.load(info.getFileName(), BitmapFont.class);
                                assetMaster.loadedAssets.put(info.getAssetName(name), info.getFileName());
                                break;
                            case "truetypefont":

                                break;
                            case "systemtruetypefont": //loads from system fonts.
                                if (info.getParams() != null)
                                {
                                    params = info.getParams().split(" ");
                                    for (String parameter : params)
                                    {
                                        String[] args = parameter.split("_");

                                        switch (args[0])
                                        {
                                            case "s":
                                                TrueTypeFontGenerator.getParameters().size = Integer.parseInt(args[1]);
                                                break;
                                            case "x":
                                                TrueTypeFontGenerator.getParameters().spaceX = Integer.parseInt(args[1]);
                                                break;
                                            case "y":
                                                TrueTypeFontGenerator.getParameters().spaceY = Integer.parseInt(args[1]);
                                                break;
                                            case "k":
                                                TrueTypeFontGenerator.getParameters().kerning = Integer.parseInt(args[1]) != 0;
                                                break;
                                        }
                                    }
                                }
                                assetMaster.loadedFonts.put(info.getAssetName(name), TrueTypeFontGenerator.generateSystemFont(info.getFileName()));
                                break;
                            case "special":
                                if (info.getParams() != null)
                                {
                                    params = info.getParams().split(" ");
                                    SpecialLoader loader = assetMaster.specialLoaders.get(params[0]);

                                    if (loader != null)
                                    {
                                        //use manager.load(filename, Texture.class) to load background images for special
                                        loader.load(name, manager, info, params);
                                        //osu! folder /Data/bg
                                    }
                                    else
                                    {
                                        editorLogger.error("Special loader " + params[0] + " not found for file " + info.getFileName());
                                    }
                                }
                                break;
                            default:
                                editorLogger.error("Attempted to load asset of unknown type: " + info.getAssetName(name) + " of type " + info.getType() + " with file " + info.getFileName());
                                break;
                        }
                    }
                    catch (Exception e)
                    {
                        editorLogger.error("Failed to load asset: " + info.getAssetName(name) + " of type " + info.getType() + " with file " + info.getFileName());
                        e.printStackTrace();
                    }
                }

                loadedLists.add(name);
            }
        }
        else
        {
            editorLogger.error("LIST NOT FOUND: " + name);
        }
    }

    public void unloadList(String name, AssetMaster master)
    {
        if (lists.containsKey(name) && loadedLists.contains(name))
        {
            editorLogger.info("Unloading asset list \"" + name + "\"");

            for (AssetInfo info : lists.get(name))
            {
                switch (info.getType())
                {
                    case "region":
                        if (assetMaster.textureRegions.containsKey(info.getFileName()))
                        {
                            for (String regionName : assetMaster.textureRegions.get(info.getFileName()))
                            {
                                assetMaster.loadedRegions.remove(regionName);
                            }
                        }
                        master.unload(info.getFileName());
                        break;
                    case "special":
                        if (info.getParams() != null)
                        {
                            String[] params = info.getParams().split(" ");
                            SpecialLoader loader = assetMaster.specialLoaders.get(params[0]);

                            if (loader != null)
                            {
                                loader.unload(name, info, params);
                            }
                            else
                            {
                                editorLogger.error("Special loader " + params[0] + " not found for file " + info.getFileName());
                            }
                        }
                    case "font":
                        editorLogger.warn("Fonts shouldn't be unloaded! Trying to unload font " + info.getAssetName(name));
                    default:
                        master.unload(info.getAssetName(name));
                }
            }

            loadedLists.remove(name);
        }
    }

    public void addList(String name, ArrayList<AssetInfo> assets)
    {
        lists.put(name, assets);
    }

    public void addLists(AssetLists additionalLists)
    {
        lists.putAll(additionalLists.lists);
    }

    @Override
    public void write(Json json) {
        for (Map.Entry<String, ArrayList<AssetInfo>> info : lists.entrySet())
        {
            json.writeArrayStart(info.getKey());

            for (AssetInfo i : info.getValue())
            {
                json.writeObjectStart();
                i.write(json);
                json.writeObjectEnd();
            }

            json.writeArrayEnd();
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        for (JsonValue entry = jsonData.child; entry != null; entry = entry.next)
        {
            ArrayList<AssetInfo> infoList = new ArrayList<>();
            for (JsonValue info = entry.child; info != null; info = info.next)
            {
                AssetInfo read = new AssetInfo();
                read.read(json, info);
                infoList.add(read);
            }

            lists.put(entry.name, infoList);
        }
    }

    public int count()
    {
        return lists.size();
    }
}
