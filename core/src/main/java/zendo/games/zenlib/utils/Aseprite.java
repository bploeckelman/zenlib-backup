package zendo.games.zenlib.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.GdxRuntimeException;
import zendo.games.zenlib.assets.Sprite;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A simple Aseprite file parser
 * @link https://github.com/aseprite/aseprite/blob/master/docs/ase-file-specs.md
 *
 * Based on NoelFB's Blah framework
 * @link https://github.com/noelfb/blah
 */
public class Aseprite {

    private static final String tag = Aseprite.class.getSimpleName();

    // ----------------------------------------------------
    // enumerations
    // ----------------------------------------------------

    enum Modes {
          indexed   (1)
        , grayscale (2)
        , rgba      (4)
        ;
        public final int value;
        Modes(int value) { this.value = value; }

        public static Modes fromValue(int value) {
            switch (value) {
                case 1: return indexed;
                case 2: return grayscale;
                case 4: return rgba;
                default: throw new GdxRuntimeException("Invalid Aseprite.Modes value: " + value);
            }
        }
    }

    enum Chunks {
          OldPaletteA  (0x0004) // ignore if Palette (0x2019) is found
        , OldPaletteB  (0x0011) // ignore if Palette (0x2019) is found
        , Layer        (0x2004)
        , Cel          (0x2005)
        , CelExtra     (0x2006)
        , ColorProfile (0x2007)
        , Mask         (0x2016) // deprecated
        , Path         (0x2017) // never used
        , FrameTags    (0x2018)
        , Palette      (0x2019)
        , UserData     (0x2020)
        , Slice        (0x2022)
        ;
        public final int value;
        Chunks(int value) { this.value = value; }

        public static Chunks fromValue(int value) {
            switch (value) {
                case 0x0004: return OldPaletteA;
                case 0x0011: return OldPaletteB;
                case 0x2004: return Layer;
                case 0x2005: return Cel;
                case 0x2006: return CelExtra;
                case 0x2007: return ColorProfile;
                case 0x2016: return Mask;
                case 0x2017: return Path;
                case 0x2018: return FrameTags;
                case 0x2019: return Palette;
                case 0x2020: return UserData;
                case 0x2022: return Slice;
                default: throw new GdxRuntimeException("Invalid Aseprite.Chunks value: " + value);
            }
        }
    }

    enum LoopDirections {
          Forward(0)
        , Reverse(1)
        , PingPong(2)
        ;
        public final int value;
        LoopDirections(int value) { this.value = value; }

        public static LoopDirections fromValue(int value) {
            switch (value) {
                case 0: return Forward;
                case 1: return Reverse;
                case 2: return PingPong;
                default: throw new GdxRuntimeException("Invalid Aseprite.LoopDirections value: " + value);
            }
        }
    }

    enum LayerTypes {
          Normal(0)
        , Group(1)
        ;
        public final int value;
        LayerTypes(int value) { this.value = value; }

        // *sad trombone* why you gotta be this way java
        public static LayerTypes fromValue(int value) {
            switch (value) {
                case 0: return Normal;
                case 1: return Group;
                default: throw new GdxRuntimeException("Invalid Aseprite.LayerTypes value: " + value);
            }
        }
    }

    // ----------------------------------------------------
    // bitmask constants
    // ----------------------------------------------------

    static final int layer_flag_visible          = 1 << 0;
    static final int layer_flag_editable         = 1 << 1;
    static final int layer_flag_lockmovement     = 1 << 2;
    static final int layer_flag_backgroun        = 1 << 3;
    static final int layer_flag_preferlinkedcels = 1 << 4;
    static final int layer_flag_collapsed        = 1 << 5;
    static final int layer_flag_reference        = 1 << 6;


    // ----------------------------------------------------
    // structs
    // ----------------------------------------------------

    public static class UserData {
        public String text = null;
        public Color color = null;
    }

    public static class Cel {
        public int layer_index = 0;
        public int linked_frame_index = 0;
        public int x = 0;
        public int y = 0;
        public byte alpha = 0;
        public Pixmap image = null;
        public UserData userdata = null;
    }

    public static class Frame {
        public int duration = 0;
        public Pixmap image = null;
        public List<Cel> cels = null;
    }

    public static class Layer {
        public int flags = 0;
        public LayerTypes type = LayerTypes.Normal;
        public String name = "";
        public int child_level = 0;
        public int blendmode = 0;
        public byte alpha = 0;
        public boolean visible = true;
        public UserData userdata = null;
    }

    public static class Tag {
        public String name = "";
        public LoopDirections loops = LoopDirections.Forward;
        public int from = 0;
        public int to = 0;
        public Color color = Color.WHITE.cpy();
        public UserData userdata = null;
    }

    public static class Slice {
        public int frame = 0;
        public String name = "";
        public Point origin = Point.at(0, 0);
        public int width = 0;
        public int height = 0;
        public boolean has_pivot = false;
        public Point pivot = Point.at(0, 0);
        public UserData userdata = null;
    }

    // ----------------------------------------------------
    // fields
    // ----------------------------------------------------

    public Modes mode = Modes.rgba;
    public int width  = 0;
    public int height = 0;

    public ArrayList<Layer> layers  = new ArrayList<>();
    public ArrayList<Frame> frames  = new ArrayList<>();
    public ArrayList<Tag>   tags    = new ArrayList<>();
    public ArrayList<Slice> slices  = new ArrayList<>();
    public ArrayList<Color> palette = new ArrayList<>();

    private UserData lastUserdata = null;

    // ----------------------------------------------------
    // constructors
    // ----------------------------------------------------

    public Aseprite(String path) {
        this(Gdx.files.internal(path));
    }

    public Aseprite(FileHandle file) {
        parse(file);
    }

    // ----------------------------------------------------
    // loading helper
    // ----------------------------------------------------

    /**
     * Data required to create a Sprite from an Aseprite file and packed textures
     */
    public static class SpriteInfo {
        public String path;
        public String name;
        public Aseprite aseprite;
        public Map<String, AnimFrameInfo[]> anim_frame_infos;

        public static class AnimFrameInfo {
            public String region_name;
            public int region_index;
            public float duration;
        }
    }

    /**
     * Load the Aseprite file specified by 'path', packing animation frames
     * with the specified 'packer' and disposing of loaded Pixmap data from
     * the Aseprite files after it is packed
     *
     * @param packer a configured PixmapPacker used to pack animation frame data
     * @param path the path of the Aseprite file to load
     *
     * @return a SpriteInfo object populated with details of the loaded Aseprite file
     *         and references for how to find the TextureRegions packed by the PixmapPacker
     */
    public static SpriteInfo loadAndPack(PixmapPacker packer, String path) {
        var info = new SpriteInfo();
        {
            info.path = path;
            info.name = path.subSequence(path.lastIndexOf('/') + 1, path.indexOf(".ase")).toString();
            info.aseprite = new Aseprite(path);
            info.anim_frame_infos = new HashMap<>();

            // build animation info for each tag
            for (var anim_tag : info.aseprite.tags) {
                var num_frames = anim_tag.to - anim_tag.from + 1;

                // build frame infos for each frame of this animation
                info.anim_frame_infos.putIfAbsent(anim_tag.name, new SpriteInfo.AnimFrameInfo[num_frames]);
                for (int i = 0; i < num_frames; i++) {
                    int frame_index = anim_tag.from + i;

                    // collect frame information from the aseprite file
                    // note:
                    //  the string used for atlas.findRegion must _not_ include the frame index
                    //  while the string used to pack a region into the atlas _must_ include the frame index
                    var frame = info.aseprite.frames.get(frame_index);
                    var frame_region_name = info.name + "-" + anim_tag.name;
                    var frame_region_name_w_index = frame_region_name + "_" + i;
                    var frame_duration = frame.duration;

                    // pack the frame image into the texture atlas
                    packer.pack(frame_region_name_w_index, frame.image);

                    // save the info needed to build the sprite's animation for this tag/frame
                    var anim_frame_infos = info.anim_frame_infos.get(anim_tag.name);
                    anim_frame_infos[i] = new SpriteInfo.AnimFrameInfo();
                    anim_frame_infos[i].region_name = frame_region_name;
                    anim_frame_infos[i].region_index = i;
                    anim_frame_infos[i].duration = frame_duration;
                }
            }

            // dispose Aseprite Pixmap images since they are now packed into the texture atlas
            for (var frame : info.aseprite.frames) {
                for (var cel : frame.cels) {
                    cel.image.dispose();
                }
                frame.image.dispose();
            }
        }
        return info;
    }

    /**
     * Create a Sprite object based on the specified SpriteInfo with animation frames
     * pulled from the specified TextureAtlas
     *
     * @param info the data required to create a Sprite
     * @param atlas the TextureAtlas that holds animation frame TextureRegions referred
     *              to by the specified SpriteInfo
     *
     * @return a Sprite object populated based on data specified in SpriteInfo
     */
    public static Sprite createSprite(Aseprite.SpriteInfo info, TextureAtlas atlas) {
        var sprite = new Sprite();
        {
            // extract properties from aseprite
            sprite.name = info.name;
            sprite.origin.set(0, 0);
            if (info.aseprite.slices.size() > 0 && info.aseprite.slices.get(0).has_pivot) {
                var slice = info.aseprite.slices.get(0);
                // flip slice pivot point to be y-up to match in-game reference with aseprite pivot point
                sprite.origin.set(slice.pivot.x, slice.pivot.y - slice.height);
            }

            // build sprite animations
            for (var anim_name : info.anim_frame_infos.keySet()) {
                var anim_frame_info = info.anim_frame_infos.get(anim_name);

                // build frames for animation
                var anim_frames = new Sprite.Frame[anim_frame_info.length];
                for (int i = 0; i < anim_frame_info.length; i++) {
                    var frame_info = anim_frame_info[i];
                    var frame_region = atlas.findRegion(frame_info.region_name, frame_info.region_index);
                    var frame_duration = anim_frame_info[i].duration;
                    anim_frames[i] = new Sprite.Frame(frame_region, frame_duration / 1000f);
                }

                // build animation from frames
                var anim = new Sprite.Anim(anim_name, anim_frames);

                // add to sprite
                sprite.animations.add(anim);
            }
        }
        return sprite;
    }

    // ----------------------------------------------------
    // implementation
    // ----------------------------------------------------

    private void parse(FileHandle file) {
        Gdx.app.debug(tag, "Loading file: " + file.path());
        if (!file.exists()) {
            throw new GdxRuntimeException("Aseprite file does not exist: " + file.path());
        }

        // create byte buffer from file contents and set endianness for .ase files
        var bytes = file.readBytes();
        var stream = ByteBuffer.wrap(bytes);
        stream.order(ByteOrder.LITTLE_ENDIAN);

        int frame_count = 0;

        // header
        {
            // extract filesize, but it's unused so don't store it
            stream.getInt();

            // extract and validate magic number
            var magic = stream.getShort();
            if (magic != (short)0xA5E0) {
                throw new GdxRuntimeException("File is not a valid Aseprite file (bad header magic): " + file.path());
            }

            // extract main data
            frame_count = stream.getShort();
            width = stream.getShort();
            height = stream.getShort();
            mode = Modes.fromValue(stream.getShort() / 8);

            // don't care about other info, extract and drop on the floor
            stream.getInt();   // flags
            stream.getShort(); // speed (deprecated)
            stream.getInt();   // should be 0
            stream.getInt();   // should be 0
            stream.get();      // palette entry
            stream.position(stream.position() + 3); // skip reserved bytes
            stream.getShort(); // number of colors (0 means 256 for old sprites)
            stream.get();      // pixel width
            stream.get();      // pixel height
            stream.position(stream.position() + 92); // skip reserved bytes
        }

        // instantiate frames to be parsed
        for (int i = 0; i < frame_count; i++) {
            frames.add(new Frame());
        }

        // parse frames
        for (int i = 0; i < frame_count; i++) {
            var frameStart = stream.position();
            var frameSize = stream.getInt();
            var frameEnd = frameStart + frameSize;
            var chunks = 0;

            // frame header
            {
                // extract and validate magic number
                var magic = stream.getShort();
                if (magic != (short)0xF1FA) {
                    throw new GdxRuntimeException("File is not a valid Aseprite file (bad chunk magic): " + file.path());
                }

                // extract chunk counts (both old and new) and frame duration
                var old_chunk_count = stream.getShort();
                frames.get(i).duration    = stream.getShort();
                stream.position(stream.position() + 2); // skip reserved bytes
                var new_chunk_count   = stream.getInt();

                // set number of chunks, using the appropriate chunk count for the file
                if (old_chunk_count == (short)0xFFFF) {
                    chunks = new_chunk_count;
                } else {
                    chunks = old_chunk_count;
                }
            }

            // create the frame image
            frames.get(i).image = new Pixmap(width, height, Pixmap.Format.RGBA8888);

            // frame chunks
            for (int j = 0; j < chunks; j++) {
                var chunkStart = stream.position();
                var chunkEnd = chunkStart + stream.getInt();
                var chunkType = Chunks.fromValue(stream.getShort());

                switch (chunkType) {
                    case Layer:     parse_layer     (stream, i);           break;
                    case Cel:       parse_cel       (stream, i, chunkEnd); break;
                    case Palette:   parse_palette   (stream, i);           break;
                    case UserData:  parse_user_data (stream, i);           break;
                    case FrameTags: parse_tag       (stream, i);           break;
                    case Slice:     parse_slice     (stream, i);           break;
                    default: Gdx.app.debug(tag, "Ignoring chunk: " + chunkType.name());
                }

                stream.position(chunkEnd);
            }

            // move to end of frame
            stream.position(frameEnd);
        }

        Gdx.app.log(tag, "File loaded: " + file.path());
    }

    private void parse_layer(ByteBuffer stream, int frame) {
        var layer = new Layer();
        {
            layer.flags = stream.getShort();
            layer.visible     = ((layer.flags & layer_flag_visible) == layer_flag_visible);
            layer.type        = LayerTypes.fromValue(stream.getShort());
            layer.child_level = stream.getShort();
            stream.getShort(); // skip width
            stream.getShort(); // skip height
            layer.blendmode   = stream.getShort();
            layer.alpha       = stream.get();
            stream.position(stream.position() + 3); // skip reserved bytes

            var nameLength = stream.getShort();
            var nameBytes = new byte[nameLength];
            stream.get(nameBytes, 0, nameLength);
            layer.name = new String(nameBytes);

            layer.userdata = new UserData();
            layer.userdata.color = Color.WHITE.cpy();
            layer.userdata.text = "";
            lastUserdata = layer.userdata;
        }
        layers.add(layer);
    }

    private void parse_cel(ByteBuffer stream, int frameIndex, int maxPosition) {
        var frame = frames.get(frameIndex);
        if (frame.cels == null) {
            frame.cels = new ArrayList<>();
        }

        var cel = new Cel();
        {
            cel.layer_index = stream.getShort();
            cel.x = stream.getShort();
            cel.y = stream.getShort();
            cel.alpha = stream.get();
            cel.linked_frame_index = -1;

            var cel_type = stream.getShort();
            stream.position(stream.position() + 7); // skip reserved bytes

            // RAW or DEFLATE
            if (cel_type == 0 || cel_type == 2) {
                var width  = stream.getShort();
                var height = stream.getShort();
                var num_image_bytes = width * height * mode.value;

                // create the backing pixmap
                cel.image = new Pixmap(width, height, Pixmap.Format.RGBA8888);
                var imageBytes = ByteBuffer.allocate(num_image_bytes);

                // load pixels in rgba format
                // RAW
                if (cel_type == 0) {
                    stream.get(imageBytes.array(), 0, num_image_bytes);
                }
                // DEFLATE
                else {
                    // try to decode the pixel bytes
                    try {
                        // note - in noel's parser he clamps this value at INT32_MAX
                        //        not sure how the value could get bigger since its the diff of 2 ints
                        var size = maxPosition - stream.position();
                        var buffer = new byte[size];
                        stream.get(buffer, 0, size);

                        // sizeof Color in bytes = 4
                        var output_length = width * height * 4;

                        var inflater = new Inflater();
                        inflater.setInput(buffer, 0, size);
                        inflater.inflate(imageBytes.array(), 0, output_length);
                    } catch (DataFormatException e) {
                        throw new GdxRuntimeException("File is not a valid Aseprite file (unable to inflate cel pixel data for frame): " + frameIndex);
                    }
                }

                // todo - review these conversions, they're probably not right

                // convert rgba loaded pixels to another format if mode is not rgba
                // note - we work in-place to save having to store stuff in a buffer
                if (mode == Modes.grayscale) {
                    Gdx.app.log(tag, "converting cel pixels to grayscale not yet implemented");
//                    var src = cel.image.getPixels().array();
//                    var dst = cel.image.getPixels().array();
//
//                    for (int d = width * height - 1, s = (width * height - 1) * 2; d >= 0; d--, s -= 2) {
//                        dst[d] = new Color(src[s], src[s], src[s], src[s + 1]);
//                    }
                }
                else if (mode == Modes.indexed) {
                    Gdx.app.log(tag, "possibly broken: converting cel pixels to indexed colors....");
                    var src = imageBytes;
                    var dst = imageBytes;
                    for (int i = src.array().length - 1; i >= 0; i -= 4) {
                        // TODO: double check byte ordering, this is a bit oof
                        // convert source bytes into integer palette index
                        var srcBytes = new byte[] {src.get(i), src.get(i-1), src.get(i-2), src.get(i-3)};
                        var palette_index = ByteBuffer.wrap(srcBytes).getInt();

                        // retrieve the indexed color from the previously loaded palette
                        var indexed_color = palette.get(palette_index);

                        // convert indexed color to int bytes and write back to dst
                        var result = ByteBuffer.allocate(4).putInt(indexed_color.toIntBits()).array();
                        dst.put(i - 0, result[0]);
                        dst.put(i - 1, result[1]);
                        dst.put(i - 2, result[2]);
                        dst.put(i - 3, result[3]);
                    }
                }

                // update the pixels in the cel's pixmap
                cel.image.getPixels().put(imageBytes);
            }
            // REFERENCE (this cel directly references a previous cel)
            else if (cel_type == 1) {
                cel.linked_frame_index = stream.getShort();
            }

            // draw to frame if visible
            if ((layers.get(cel.layer_index).flags & layer_flag_visible) != 0) {
                render_cel(cel, frame);
            }

            // update userdata
            cel.userdata = new UserData();
            cel.userdata.color = Color.WHITE.cpy();
            cel.userdata.text = "";
            lastUserdata = cel.userdata;
        }
        frame.cels.add(cel);
    }

    private void parse_palette(ByteBuffer stream, int frame) {
        stream.getInt(); // size
        var start = stream.getInt();
        var end   = stream.getInt();
        stream.position(stream.position() + 8); // skip reserved bytes

        var newSize = palette.size() + (end - start) + 1;
        palette.ensureCapacity(newSize);

        for (int p = 0, len = (end - start) + 1; p < len; p++) {
            var hasName = stream.getShort();

            // colors are stored in big endian order
            // so temporarily reverse byte order to read the color out
            stream.order(ByteOrder.BIG_ENDIAN);
            palette.add(start + p, new Color(stream.getInt()));
            stream.order(ByteOrder.LITTLE_ENDIAN);

            if ((hasName & 0xF000) != 0) {
                len = stream.getShort();
                stream.position(stream.position() + len);
            }
        }
    }

    private void parse_user_data(ByteBuffer stream, int frame) {
        if (lastUserdata != null) {
            var flags = stream.getInt();

            // has text
            if ((flags & (1 << 0)) != 0) {
                var textLength = stream.getShort();
                var textBytes = new byte[textLength];
                stream.get(textBytes, 0, textLength);
                lastUserdata.text = new String(textBytes);
            }

            // has color
            if ((flags & (1 << 1)) != 0) {
                // colors are stored in big endian order
                // so temporarily reverse byte order to read the color out
                stream.order(ByteOrder.BIG_ENDIAN);
                lastUserdata.color = new Color(stream.getInt());
                stream.order(ByteOrder.LITTLE_ENDIAN);
            }
        }
    }

    private void parse_tag(ByteBuffer stream, int frame) {
        var num_tags = stream.getShort();
        stream.position(stream.position() + 8); // skip reserved bytes

        for (int i = 0; i < num_tags; i++) {
            Tag tag = new Tag();
            {
                tag.from = stream.getShort();
                tag.to   = stream.getShort();
                tag.loops = LoopDirections.fromValue(stream.get());
                stream.position(stream.position() + 8); // skip reserved bytes

                // note - this might not be correct
                //        the spec shows byte[3] (rgb tag color) then byte[1] (extra zero byte)
                //        not sure if the extra zero byte gets picked up as the alpha byte
                // colors are stored in big endian order
                // so temporarily reverse byte order to read the color out
                stream.order(ByteOrder.BIG_ENDIAN);
                tag.color = new Color(stream.getInt());
                tag.color.a = 1f;
                stream.order(ByteOrder.LITTLE_ENDIAN);

                var nameLength = stream.getShort();
                var nameBytes = new byte[nameLength];
                stream.get(nameBytes, 0, nameLength);
                tag.name = new String(nameBytes);
            }
            tags.add(tag);
        }
    }

    private void parse_slice(ByteBuffer stream, int frame) {
        var num_slices = stream.getInt();
        var flags      = stream.getInt();
        stream.getInt(); // skip reserved bytes

        var nameLength = stream.getShort();
        var nameBytes = new byte[nameLength];
        stream.get(nameBytes, 0, nameLength);
        var name = new String(nameBytes);

        for (int i = 0; i < num_slices; i++) {
            Slice slice = new Slice();
            {
                slice.name     = name;
                slice.frame    = stream.getInt();
                slice.origin.x = stream.getInt();
                slice.origin.y = stream.getInt();
                slice.width    = stream.getInt();
                slice.height   = stream.getInt();

                // 9 slice (ignored for now)
                if ((flags & (1 << 0)) != 0) {
                    stream.getInt();
                    stream.getInt();
                    stream.getInt();
                    stream.getInt();
                }

                // pivot point
                slice.has_pivot = false;
                if ((flags & (1 << 1)) != 0) {
                    slice.has_pivot = true;
                    slice.pivot.x = stream.getInt();
                    slice.pivot.y = stream.getInt();
                }

                slice.userdata = new UserData();
                slice.userdata.color = Color.WHITE.cpy();
                slice.userdata.text = "";
                lastUserdata = slice.userdata;
            }
            slices.add(slice);
        }
    }

    // note - since a frame could contain multiple cells
    //        this method should composite the specified cell
    //        into the existing frame image rather than
    //        just overwriting the existing frame image
    private void render_cel(Cel cel, Frame frame) {
        frame.image.drawPixmap(cel.image, cel.x, cel.y);
    }

}
