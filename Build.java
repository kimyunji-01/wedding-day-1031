// 사용법: media/<스팟id>/ 폴더에 사진·영상을 넣고, 프로젝트 루트에서 `java Build.java`
// (JDK 11 이상) → 썸네일(thumbs/)과 items.json 이 자동으로 만들어져요.
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class Build {
    static final Set<String> IMG = Set.of("jpg", "jpeg", "png"), VID = Set.of("mp4", "webm", "mov");
    static final Pattern NUM = Pattern.compile("\\d+");

    // 파일명 속 숫자를 자리수 맞춰서 정렬 (img2 < img10)
    static String key(Path p) {
        return NUM.matcher(p.getFileName().toString().toLowerCase())
                .replaceAll(m -> String.format("%018d", Long.parseLong(m.group().length() > 18 ? "0" : m.group())));
    }

    static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get("").toAbsolutePath(), media = root.resolve("media");
        List<Path> spots = new ArrayList<>();
        try (var s = Files.list(media)) { s.filter(Files::isDirectory).sorted().forEach(spots::add); }

        List<String> spotJson = new ArrayList<>();
        for (Path sp : spots) {
            String id = sp.getFileName().toString();
            List<Path> files = new ArrayList<>();
            try (var s = Files.list(sp)) { s.filter(Files::isRegularFile).forEach(files::add); }
            files.sort(Comparator.comparing(Build::key));

            List<String> items = new ArrayList<>();
            for (Path f : files) {
                String name = f.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot < 0) continue;
                String stem = name.substring(0, dot), ext = name.substring(dot + 1).toLowerCase();
                String src = root.relativize(f).toString().replace('\\', '/');
                if (IMG.contains(ext)) {
                    Path t = root.resolve("thumbs").resolve(id).resolve(stem + ".jpg");
                    if (!Files.exists(t) || Files.getLastModifiedTime(t).compareTo(Files.getLastModifiedTime(f)) < 0) {
                        Files.createDirectories(t.getParent());
                        BufferedImage im = ImageIO.read(f.toFile());
                        if (im == null) { System.out.println("  ! 읽을 수 없는 이미지(건너뜀): " + name); continue; }
                        ImageIO.write(rotate(shrink(im, 640), orientation(f.toFile())), "jpg", t.toFile());
                    }
                    items.add("{\"type\":\"image\",\"src\":\"" + esc(src) + "\",\"thumb\":\"" + esc(root.relativize(t).toString().replace('\\', '/')) + "\"}");
                } else if (VID.contains(ext)) {
                    if (ext.equals("mov")) System.out.println("  ! .mov 는 일부 폰에서 재생이 안 돼요. mp4(H.264) 변환 추천: " + name);
                    items.add("{\"type\":\"video\",\"src\":\"" + esc(src) + "\"}");
                } else if (!name.startsWith(".")) {
                    System.out.println("  ! 지원하지 않는 형식(건너뜀): " + name);
                }
            }
            System.out.println(id + ": " + items.size() + "개");
            spotJson.add("\"" + esc(id) + "\": [\n  " + String.join(",\n  ", items) + "\n ]");
        }
        Files.writeString(root.resolve("items.json"), "{\n" + String.join(",\n", spotJson) + "\n}\n", StandardCharsets.UTF_8);
    }

    static BufferedImage shrink(BufferedImage s, int max) {
        int w = s.getWidth(), h = s.getHeight();
        double r = (double) max / Math.max(w, h);
        if (r >= 1) r = 1;
        int nw = Math.max(1, (int) Math.round(w * r)), nh = Math.max(1, (int) Math.round(h * r));
        BufferedImage d = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = d.createGraphics();
        g.drawImage(s.getScaledInstance(nw, nh, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return d;
    }

    // 폰 사진의 EXIF 회전값(1,3,6,8)을 읽어서 썸네일 방향을 바로잡아요
    static BufferedImage rotate(BufferedImage s, int o) {
        int q = o == 6 ? 1 : o == 3 ? 2 : o == 8 ? 3 : 0;
        if (q == 0) return s;
        int w = s.getWidth(), h = s.getHeight();
        boolean swap = q != 2;
        BufferedImage d = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = d.createGraphics();
        AffineTransform t = new AffineTransform();
        t.translate(d.getWidth() / 2.0, d.getHeight() / 2.0);
        t.rotate(Math.toRadians(90 * q));
        t.translate(-w / 2.0, -h / 2.0);
        g.drawImage(s, t, null);
        g.dispose();
        return d;
    }

    static int orientation(File f) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            if (in.readUnsignedShort() != 0xFFD8) return 1;
            while (true) {
                int m = in.readUnsignedShort(), len = in.readUnsignedShort();
                if (m == 0xFFE1) {
                    byte[] b = new byte[len - 2];
                    in.readFully(b);
                    if (b.length < 14 || b[0] != 'E') return 1;
                    boolean le = b[6] == 'I';
                    int off = u32(b, 10, le) + 6, n = u16(b, off, le);
                    for (int i = 0; i < n; i++) {
                        int e = off + 2 + i * 12;
                        if (u16(b, e, le) == 0x0112) return u16(b, e + 8, le);
                    }
                    return 1;
                }
                if ((m & 0xFF00) != 0xFF00 || m == 0xFFDA) return 1;
                in.readFully(new byte[len - 2]);
            }
        } catch (Exception e) { return 1; }
    }
    static int u16(byte[] b, int p, boolean le) {
        return le ? (b[p] & 0xFF) | (b[p + 1] & 0xFF) << 8 : (b[p] & 0xFF) << 8 | (b[p + 1] & 0xFF);
    }
    static int u32(byte[] b, int p, boolean le) {
        return le ? u16(b, p, true) | u16(b, p + 2, true) << 16 : u16(b, p, false) << 16 | u16(b, p + 2, false);
    }
}
