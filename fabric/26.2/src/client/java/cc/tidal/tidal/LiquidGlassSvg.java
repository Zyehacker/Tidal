/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

package cc.tidal.tidal;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LiquidGlassSvg {
    private static final Pattern GRADIENT_REFERENCE = Pattern.compile("url\\(#([^)]*)\\)");

    private LiquidGlassSvg() {}

    static Scene parse(String source, String assetPath) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(
                    source.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();
            if (!"svg".equals(root.getTagName())) {
                throw unsupported(assetPath, root.getTagName());
            }
            validateAttributes(root, Set.of("xmlns", "version", "width", "height", "viewBox",
                    "fill", "fill-opacity", "stroke", "stroke-opacity", "stroke-width",
                    "stroke-linecap", "stroke-linejoin", "stroke-dasharray",
                    "stroke-dashoffset"), assetPath);
            double[] viewBox = numbers(required(root, "viewBox", assetPath));
            if (viewBox.length != 4 || viewBox[2] <= 0 || viewBox[3] <= 0) {
                throw new IllegalArgumentException("Invalid SVG viewBox in " + assetPath);
            }

            Map<String, Paint> gradients = new HashMap<>();
            validateDefinitions(root, assetPath);
            NodeList defs = root.getElementsByTagName("linearGradient");
            for (int i = 0; i < defs.getLength(); i++) {
                Element gradient = (Element) defs.item(i);
                gradients.put(required(gradient, "id", assetPath),
                        parseGradient(gradient, assetPath, viewBox[2], viewBox[3]));
            }

            List<Shape> shapes = new ArrayList<>();
            boolean usesCurrentColor = false;
            Style inherited = new Style(root.getAttribute("fill"),
                    opacity(root, "fill-opacity", 1), root.getAttribute("stroke"),
                    opacity(root, "stroke-opacity", 1),
                    (float) optionalNumber(root, "stroke-width", 1),
                    lineCap(root.getAttribute("stroke-linecap"), assetPath),
                    lineJoin(root.getAttribute("stroke-linejoin"), assetPath),
                    root.getAttribute("stroke-dasharray"),
                    (float) optionalNumber(root, "stroke-dashoffset", 0));
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element element && !"defs".equals(element.getTagName())) {
                    parseElement(element, assetPath, gradients, inherited,
                            new java.awt.geom.AffineTransform(), shapes);
                }
            }
            for (Shape shape : shapes) {
                usesCurrentColor |= shape.fillCurrentColor || shape.strokeCurrentColor;
            }
            return new Scene(viewBox[0], viewBox[1], viewBox[2], viewBox[3],
                    List.copyOf(shapes), usesCurrentColor);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse Liquid Glass SVG " + assetPath,
                    exception);
        }
    }

    private static void validateDefinitions(Element root, String assetPath) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element element)) continue;
            if (!"defs".equals(element.getTagName())) continue;
            validateAttributes(element, Set.of(), assetPath);
            NodeList definitions = element.getChildNodes();
            for (int j = 0; j < definitions.getLength(); j++) {
                if (!(definitions.item(j) instanceof Element definition)) continue;
                if ("filter".equals(definition.getTagName())) continue;
                if (!"linearGradient".equals(definition.getTagName())) {
                    throw unsupported(assetPath, definition.getTagName());
                }
                validateAttributes(definition, Set.of("id", "gradientUnits", "x1", "y1",
                        "x2", "y2"), assetPath);
                NodeList stops = definition.getChildNodes();
                for (int k = 0; k < stops.getLength(); k++) {
                    if (!(stops.item(k) instanceof Element stop)) continue;
                    if (!"stop".equals(stop.getTagName())) {
                        throw unsupported(assetPath, stop.getTagName());
                    }
                    validateAttributes(stop, Set.of("offset", "stop-color", "stop-opacity"),
                            assetPath);
                }
            }
        }
    }

    private static void parseElement(Element element, String assetPath,
                                     Map<String, Paint> gradients, Style inherited,
                                     java.awt.geom.AffineTransform transform,
                                     List<Shape> shapes) {
        if ("g".equals(element.getTagName())) {
            validateAttributes(element, Set.of("transform", "fill", "fill-opacity", "stroke",
                    "stroke-opacity", "stroke-width", "stroke-linecap", "stroke-linejoin",
                    "stroke-dasharray", "stroke-dashoffset"), assetPath);
            java.awt.geom.AffineTransform groupTransform =
                    new java.awt.geom.AffineTransform(transform);
            groupTransform.concatenate(parseTransform(element.getAttribute("transform"),
                    assetPath));
            Style groupStyle = inheritedStyle(element, inherited, assetPath);
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element child) {
                    parseElement(child, assetPath, gradients, groupStyle,
                            groupTransform, shapes);
                }
            }
            return;
        }
        parseShape(element, assetPath, gradients, inherited, transform, shapes);
    }

    private static java.awt.geom.AffineTransform parseTransform(String value, String assetPath) {
        if (value == null || value.isBlank()) return new java.awt.geom.AffineTransform();
        Matcher matcher = Pattern.compile("translate\\(([^)]*)\\)").matcher(value.trim());
        if (!matcher.matches()) throw unsupported(assetPath, "transform " + value);
        double[] translation = numbers(matcher.group(1));
        if (translation.length < 1 || translation.length > 2) {
            throw new IllegalArgumentException("Invalid SVG translation in " + assetPath);
        }
        return java.awt.geom.AffineTransform.getTranslateInstance(translation[0],
                translation.length == 2 ? translation[1] : 0);
    }

    private static Style inheritedStyle(Element element, Style inherited, String assetPath) {
        return new Style(inheritedValue(element, "fill", inherited.fillValue),
                opacity(element, "fill-opacity", inherited.fillOpacity),
                inheritedValue(element, "stroke", inherited.strokeValue),
                opacity(element, "stroke-opacity", inherited.strokeOpacity),
                (float) optionalNumber(element, "stroke-width", inherited.strokeWidth),
                element.hasAttribute("stroke-linecap")
                        ? lineCap(element.getAttribute("stroke-linecap"), assetPath)
                        : inherited.lineCap,
                element.hasAttribute("stroke-linejoin")
                        ? lineJoin(element.getAttribute("stroke-linejoin"), assetPath)
                        : inherited.lineJoin,
                inheritedValue(element, "stroke-dasharray", inherited.strokeDashArray),
                (float) optionalNumber(element, "stroke-dashoffset",
                        inherited.strokeDashOffset));
    }

    private static void parseShape(Element element, String assetPath, Map<String, Paint> gradients,
                                   Style inherited, java.awt.geom.AffineTransform transform,
                                   List<Shape> shapes) {
        Set<String> paintAttributes = Set.of("fill", "fill-opacity", "stroke",
                "stroke-opacity", "stroke-width", "stroke-linecap", "stroke-linejoin",
                "stroke-dasharray", "stroke-dashoffset", "filter");
        Set<String> allowed = switch (element.getTagName()) {
            case "path" -> union(Set.of("d"), paintAttributes);
            case "circle" -> union(Set.of("cx", "cy", "r"), paintAttributes);
            case "rect" -> union(Set.of("x", "y", "width", "height", "rx"), paintAttributes);
            case "line" -> union(Set.of("x1", "y1", "x2", "y2"), paintAttributes);
            case "polygon" -> union(Set.of("points"), paintAttributes);
            default -> throw unsupported(assetPath, element.getTagName());
        };
        validateAttributes(element, allowed, assetPath);
        java.awt.Shape shape = switch (element.getTagName()) {
            case "path" -> new PathParser(required(element, "d", assetPath), assetPath).parse();
            case "circle" -> {
                double cx = optionalNumber(element, "cx", 0);
                double cy = optionalNumber(element, "cy", 0);
                double radius = number(element, "r", assetPath);
                yield new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2);
            }
            case "rect" -> {
                double x = optionalNumber(element, "x", 0);
                double y = optionalNumber(element, "y", 0);
                double width = number(element, "width", assetPath);
                double height = number(element, "height", assetPath);
                double radius = optionalNumber(element, "rx", 0);
                if (radius == 0) yield new Rectangle2D.Double(x, y, width, height);
                yield new RoundRectangle2D.Double(x, y, width, height,
                        Math.min(width, radius * 2), Math.min(height, radius * 2));
            }
            case "line" -> new java.awt.geom.Line2D.Double(
                    optionalNumber(element, "x1", 0), optionalNumber(element, "y1", 0),
                    optionalNumber(element, "x2", 0), optionalNumber(element, "y2", 0));
            case "polygon" -> polygon(required(element, "points", assetPath), assetPath);
            default -> throw unsupported(assetPath, element.getTagName());
        };
        if (!transform.isIdentity()) shape = transform.createTransformedShape(shape);
        String fillValue = inheritedValue(element, "fill", inherited.fillValue);
        String strokeValue = inheritedValue(element, "stroke", inherited.strokeValue);
        boolean fillCurrentColor = "currentColor".equals(fillValue);
        boolean strokeCurrentColor = "currentColor".equals(strokeValue);
        Paint fill = paint(fillValue, gradients, assetPath);
        Paint stroke = paint(strokeValue, gradients, assetPath);
        if (fill == null && stroke == null && !fillCurrentColor && !strokeCurrentColor) return;
        float strokeWidth = (float) optionalNumber(element, "stroke-width",
                inherited.strokeWidth);
        int cap = element.hasAttribute("stroke-linecap")
                ? lineCap(element.getAttribute("stroke-linecap"), assetPath) : inherited.lineCap;
        int join = element.hasAttribute("stroke-linejoin")
                ? lineJoin(element.getAttribute("stroke-linejoin"), assetPath)
                : inherited.lineJoin;
        String dashArray = inheritedValue(element, "stroke-dasharray",
                inherited.strokeDashArray);
        float dashOffset = (float) optionalNumber(element, "stroke-dashoffset",
                inherited.strokeDashOffset);
        shapes.add(new Shape(shape, fill, opacity(element, "fill-opacity",
                inherited.fillOpacity), fillCurrentColor, stroke,
                opacity(element, "stroke-opacity",
                inherited.strokeOpacity),
                strokeCurrentColor, strokeWidth, cap, join,
                parseDashArray(dashArray, assetPath), dashOffset));
    }

    private static float[] parseDashArray(String value, String assetPath) {
        if (value == null || value.isBlank() || "none".equals(value)) return null;
        double[] values = numbers(value);
        if (values.length == 0) return null;
        int length = values.length % 2 == 0 ? values.length : values.length * 2;
        float[] dash = new float[length];
        for (int i = 0; i < length; i++) {
            double entry = values[i % values.length];
            if (!Double.isFinite(entry) || entry <= 0 || entry > Float.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid stroke dash array in " + assetPath);
            }
            dash[i] = (float) entry;
        }
        return dash;
    }

    private static Path2D.Double polygon(String points, String assetPath) {
        double[] coordinates = numbers(points);
        if (coordinates.length < 6 || coordinates.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid polygon points in " + assetPath);
        }
        Path2D.Double result = new Path2D.Double(Path2D.WIND_NON_ZERO);
        result.moveTo(coordinates[0], coordinates[1]);
        for (int i = 2; i < coordinates.length; i += 2) {
            result.lineTo(coordinates[i], coordinates[i + 1]);
        }
        result.closePath();
        return result;
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        var result = new java.util.HashSet<>(first);
        result.addAll(second);
        return Set.copyOf(result);
    }

    private static void validateAttributes(Element element, Set<String> allowed,
                                           String assetPath) {
        var attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.item(i).getNodeName();
            if (!allowed.contains(name)) {
                throw unsupported(assetPath, element.getTagName() + " attribute " + name);
            }
        }
    }

    private static String inheritedValue(Element element, String attribute, String inherited) {
        return element.hasAttribute(attribute) ? element.getAttribute(attribute) : inherited;
    }

    private static int lineCap(String value, String assetPath) {
        return switch (value) {
            case "", "butt" -> BasicStroke.CAP_BUTT;
            case "round" -> BasicStroke.CAP_ROUND;
            case "square" -> BasicStroke.CAP_SQUARE;
            default -> throw unsupported(assetPath, "stroke-linecap");
        };
    }

    private static int lineJoin(String value, String assetPath) {
        return switch (value) {
            case "", "miter" -> BasicStroke.JOIN_MITER;
            case "round" -> BasicStroke.JOIN_ROUND;
            case "bevel" -> BasicStroke.JOIN_BEVEL;
            default -> throw unsupported(assetPath, "stroke-linejoin");
        };
    }

    private static Paint parseGradient(Element element, String assetPath,
                                       double viewWidth, double viewHeight) {
        String units = element.getAttribute("gradientUnits");
        if (!units.isBlank() && !"userSpaceOnUse".equals(units)) {
            throw unsupported(assetPath, "linearGradient gradientUnits");
        }
        float x1 = (float) optionalNumber(element, "x1", 0);
        float y1 = (float) optionalNumber(element, "y1", 0);
        float x2 = (float) optionalNumber(element, "x2", 0);
        float y2 = (float) optionalNumber(element, "y2", 0);
        if (units.isBlank()) {
            x1 *= (float) viewWidth;
            x2 *= (float) viewWidth;
            y1 *= (float) viewHeight;
            y2 *= (float) viewHeight;
        }
        if (x1 == x2 && y1 == y2) {
            throw new IllegalArgumentException("Zero-length gradient in " + assetPath);
        }
        List<Float> offsets = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        NodeList stops = element.getChildNodes();
        for (int i = 0; i < stops.getLength(); i++) {
            if (!(stops.item(i) instanceof Element stop)) continue;
            if (!"stop".equals(stop.getTagName())) {
                throw unsupported(assetPath, stop.getTagName());
            }
            String offsetText = required(stop, "offset", assetPath);
            boolean percentage = offsetText.endsWith("%");
            float offset = Float.parseFloat(percentage
                    ? offsetText.substring(0, offsetText.length() - 1) : offsetText);
            if (percentage) offset /= 100;
            Color color = parseColor(required(stop, "stop-color", assetPath), assetPath);
            float alpha = (float) optionalNumber(stop, "stop-opacity", 1);
            offsets.add(Math.max(0, Math.min(1, offset)));
            colors.add(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    Math.round(Math.max(0, Math.min(1, alpha)) * 255)));
        }
        if (offsets.size() < 2) {
            throw new IllegalArgumentException("Gradient needs at least two stops in " + assetPath);
        }
        float[] fractions = new float[offsets.size()];
        Color[] colorStops = colors.toArray(Color[]::new);
        int trailingEndStops = 0;
        for (int i = offsets.size() - 1; i >= 0 && offsets.get(i) == 1; i--) {
            trailingEndStops++;
        }
        float trailingOffset = 1;
        for (int i = fractions.length - 1; i >= fractions.length - trailingEndStops; i--) {
            fractions[i] = trailingOffset;
            trailingOffset = Math.nextDown(trailingOffset);
        }
        for (int i = 0; i < fractions.length - trailingEndStops; i++) {
            float offset = offsets.get(i);
            if (i > 0 && offset <= fractions[i - 1]) offset = Math.nextUp(fractions[i - 1]);
            if (offset >= trailingOffset && trailingEndStops > 0) {
                throw new IllegalArgumentException("Invalid gradient stop order in " + assetPath);
            }
            fractions[i] = offset;
        }
        return new LinearGradientPaint(x1, y1, x2, y2, fractions, colorStops,
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    private static Paint paint(String value, Map<String, Paint> gradients, String assetPath) {
        if (value == null || value.isBlank() || "none".equals(value)
                || "currentColor".equals(value)) return null;
        Matcher matcher = GRADIENT_REFERENCE.matcher(value);
        if (matcher.matches()) {
            Paint paint = gradients.get(matcher.group(1));
            if (paint == null) {
                throw new IllegalArgumentException("Unknown gradient " + value + " in " + assetPath);
            }
            return paint;
        }
        if (value.startsWith("#")) return parseColor(value, assetPath);
        throw unsupported(assetPath, "paint " + value);
    }

    private static Color parseColor(String value, String assetPath) {
        if (value.length() == 4 && value.charAt(0) == '#') {
            int red = Integer.parseInt(value.substring(1, 2), 16) * 17;
            int green = Integer.parseInt(value.substring(2, 3), 16) * 17;
            int blue = Integer.parseInt(value.substring(3, 4), 16) * 17;
            return new Color(red, green, blue);
        }
        if (value.length() == 7 && value.charAt(0) == '#') {
            return new Color(Integer.parseInt(value.substring(1), 16));
        }
        throw unsupported(assetPath, "paint " + value);
    }

    private static float opacity(Element element, String attribute, float fallback) {
        return (float) Math.max(0, Math.min(1, optionalNumber(element, attribute, fallback)));
    }

    private static BasicStroke stroke(Shape shape) {
        if (shape.dashArray == null) {
            return new BasicStroke(shape.strokeWidth, shape.lineCap, shape.lineJoin);
        }
        float dashLength = 0;
        for (float value : shape.dashArray) dashLength += value;
        float phase = shape.dashOffset % dashLength;
        if (phase < 0) phase += dashLength;
        return new BasicStroke(shape.strokeWidth, shape.lineCap, shape.lineJoin, 10.0f,
                shape.dashArray, phase);
    }

    private static double number(Element element, String attribute, String assetPath) {
        String value = required(element, attribute, assetPath);
        return Double.parseDouble(value);
    }

    private static double optionalNumber(Element element, String attribute, double fallback) {
        String value = element.getAttribute(attribute);
        return value.isBlank() ? fallback : Double.parseDouble(value);
    }

    private static String required(Element element, String attribute, String assetPath) {
        String value = element.getAttribute(attribute);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing SVG attribute " + attribute
                    + " in " + assetPath);
        }
        return value;
    }

    private static double[] numbers(String text) {
        String[] parts = text.trim().split("[\\s,]+");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) values[i] = Double.parseDouble(parts[i]);
        return values;
    }

    private static IllegalArgumentException unsupported(String assetPath, String element) {
        return new IllegalArgumentException("Unsupported SVG element or attribute " + element
                + " in " + assetPath);
    }

    record Scene(double x, double y, double width, double height, List<Shape> shapes,
                 boolean usesCurrentColor) {
        BufferedImage rasterize(int deviceWidth, int deviceHeight) {
            return rasterize(deviceWidth, deviceHeight, 1, 0xFFF3F5F8);
        }

        BufferedImage rasterize(int deviceWidth, int deviceHeight, int quality) {
            return rasterize(deviceWidth, deviceHeight, quality, 0xFFF3F5F8);
        }

        BufferedImage rasterize(int deviceWidth, int deviceHeight, int quality,
                                int currentColor) {
            if (quality < 1 || quality > 4) {
                throw new IllegalArgumentException("Invalid SVG raster quality " + quality);
            }
            int rasterWidth = Math.multiplyExact(deviceWidth, quality);
            int rasterHeight = Math.multiplyExact(deviceHeight, quality);
            BufferedImage image = new BufferedImage(rasterWidth, rasterHeight,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            graphics.scale(rasterWidth / width, rasterHeight / height);
            graphics.translate(-x, -y);
            for (Shape entry : shapes) {
                if (entry.fill != null) {
                    graphics.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, entry.fillOpacity));
                    graphics.setPaint(entry.fill);
                    graphics.fill(entry.shape);
                }
                if (entry.fillCurrentColor) {
                    graphics.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, entry.fillOpacity));
                    graphics.setColor(new Color(currentColor, true));
                    graphics.fill(entry.shape);
                }
                if (entry.stroke != null && entry.strokeWidth > 0) {
                    graphics.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, entry.strokeOpacity));
                    graphics.setPaint(entry.stroke);
                    graphics.setStroke(stroke(entry));
                    graphics.draw(entry.shape);
                }
                if (entry.strokeCurrentColor && entry.strokeWidth > 0) {
                    graphics.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, entry.strokeOpacity));
                    graphics.setColor(new Color(currentColor, true));
                    graphics.setStroke(stroke(entry));
                    graphics.draw(entry.shape);
                }
            }
            graphics.dispose();
            if (quality == 1) return image;
            BufferedImage downsampled = new BufferedImage(deviceWidth, deviceHeight,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D downsampleGraphics = downsampled.createGraphics();
            downsampleGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            downsampleGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            downsampleGraphics.drawImage(image, 0, 0, deviceWidth, deviceHeight, null);
            downsampleGraphics.dispose();
            image.flush();
            return downsampled;
        }
    }

    private record Shape(java.awt.Shape shape, Paint fill, float fillOpacity,
                         boolean fillCurrentColor, Paint stroke, float strokeOpacity,
                         boolean strokeCurrentColor, float strokeWidth,
                         int lineCap, int lineJoin, float[] dashArray, float dashOffset) {}

    private record Style(String fillValue, float fillOpacity, String strokeValue,
                         float strokeOpacity, float strokeWidth, int lineCap, int lineJoin,
                         String strokeDashArray, float strokeDashOffset) {}

    private static final class PathParser {
        private final String path;
        private final String assetPath;
        private final Path2D.Double result = new Path2D.Double(Path2D.WIND_NON_ZERO);
        private int index;
        private char command;
        private char previousCommand;
        private double x;
        private double y;
        private double controlX;
        private double controlY;
        private double quadraticControlX;
        private double quadraticControlY;
        private double moveX;
        private double moveY;

        private PathParser(String path, String assetPath) {
            this.path = path;
            this.assetPath = assetPath;
        }

        private Path2D.Double parse() {
            while (true) {
                skipSeparators();
                if (index >= path.length()) return result;
                char next = path.charAt(index);
                if (Character.isLetter(next)) {
                    command = next;
                    index++;
                } else if (command == 0) {
                    throw new IllegalArgumentException("Path command expected in " + assetPath);
                }
                execute(command);
            }
        }

        private void execute(char activeCommand) {
            boolean relative = Character.isLowerCase(activeCommand);
            char upper = Character.toUpperCase(activeCommand);
            switch (upper) {
                case 'M' -> {
                    double nx = coordinate(relative, x);
                    double ny = coordinate(relative, y);
                    result.moveTo(nx, ny);
                    x = moveX = nx;
                    y = moveY = ny;
                    command = relative ? 'l' : 'L';
                }
                case 'L' -> lineTo(coordinate(relative, x), coordinate(relative, y));
                case 'H' -> lineTo(coordinate(relative, x), y);
                case 'V' -> lineTo(x, coordinate(relative, y));
                case 'C' -> cubicTo(coordinate(relative, x), coordinate(relative, y),
                        coordinate(relative, x), coordinate(relative, y),
                        coordinate(relative, x), coordinate(relative, y));
                case 'Q' -> {
                    double controlPointX = coordinate(relative, x);
                    double controlPointY = coordinate(relative, y);
                    double nx = coordinate(relative, x);
                    double ny = coordinate(relative, y);
                    quadraticTo(controlPointX, controlPointY, nx, ny);
                }
                case 'S' -> {
                    double reflectedX = isCubic(previousCommand) ? 2 * x - controlX : x;
                    double reflectedY = isCubic(previousCommand) ? 2 * y - controlY : y;
                    cubicTo(reflectedX, reflectedY, coordinate(relative, x),
                            coordinate(relative, y), coordinate(relative, x),
                            coordinate(relative, y));
                }
                case 'T' -> {
                    double reflectedX = isQuadratic(previousCommand)
                            ? 2 * x - quadraticControlX : x;
                    double reflectedY = isQuadratic(previousCommand)
                            ? 2 * y - quadraticControlY : y;
                    quadraticTo(reflectedX, reflectedY, coordinate(relative, x),
                            coordinate(relative, y));
                }
                case 'A' -> {
                    double radiusX = number();
                    double radiusY = number();
                    double rotation = number();
                    double largeArc = number();
                    double sweep = number();
                    double nx = coordinate(relative, x);
                    double ny = coordinate(relative, y);
                    if ((largeArc != 0 && largeArc != 1) || (sweep != 0 && sweep != 1)) {
                        throw new IllegalArgumentException("Invalid SVG arc flag in " + assetPath);
                    }
                    arcTo(radiusX, radiusY, rotation, largeArc == 1, sweep == 1, nx, ny);
                    x = nx;
                    y = ny;
                    previousCommand = activeCommand;
                }
                case 'Z' -> {
                    result.closePath();
                    x = moveX;
                    y = moveY;
                    previousCommand = activeCommand;
                    command = 0;
                }
                default -> throw unsupported(assetPath, "path command " + activeCommand);
            }
            if (upper != 'A' && upper != 'Z') previousCommand = activeCommand;
        }

        private void lineTo(double nx, double ny) {
            result.lineTo(nx, ny);
            x = nx;
            y = ny;
            previousCommand = command;
        }

        private void cubicTo(double x1, double y1, double x2, double y2,
                             double nx, double ny) {
            result.curveTo(x1, y1, x2, y2, nx, ny);
            controlX = x2;
            controlY = y2;
            x = nx;
            y = ny;
            previousCommand = command;
        }

        private void quadraticTo(double controlPointX, double controlPointY,
                                 double nx, double ny) {
            result.quadTo(controlPointX, controlPointY, nx, ny);
            quadraticControlX = controlPointX;
            quadraticControlY = controlPointY;
            x = nx;
            y = ny;
            previousCommand = command;
        }

        private static boolean isQuadratic(char value) {
            return Character.toUpperCase(value) == 'Q'
                    || Character.toUpperCase(value) == 'T';
        }

        private void arcTo(double radiusX, double radiusY, double degrees, boolean largeArc,
                           boolean sweep, double endX, double endY) {
            radiusX = Math.abs(radiusX);
            radiusY = Math.abs(radiusY);
            if (radiusX == 0 || radiusY == 0 || (x == endX && y == endY)) {
                result.lineTo(endX, endY);
                return;
            }
            double phi = Math.toRadians(degrees % 360);
            double cos = Math.cos(phi);
            double sin = Math.sin(phi);
            double dx = (x - endX) / 2;
            double dy = (y - endY) / 2;
            double primeX = cos * dx + sin * dy;
            double primeY = -sin * dx + cos * dy;
            double lambda = primeX * primeX / (radiusX * radiusX)
                    + primeY * primeY / (radiusY * radiusY);
            if (lambda > 1) {
                double scale = Math.sqrt(lambda);
                radiusX *= scale;
                radiusY *= scale;
            }
            double numerator = Math.max(0, radiusX * radiusX * radiusY * radiusY
                    - radiusX * radiusX * primeY * primeY
                    - radiusY * radiusY * primeX * primeX);
            double denominator = radiusX * radiusX * primeY * primeY
                    + radiusY * radiusY * primeX * primeX;
            double factor = denominator == 0 ? 0
                    : Math.sqrt(numerator / denominator) * (largeArc == sweep ? -1 : 1);
            double centerPrimeX = factor * radiusX * primeY / radiusY;
            double centerPrimeY = factor * -radiusY * primeX / radiusX;
            double centerX = cos * centerPrimeX - sin * centerPrimeY + (x + endX) / 2;
            double centerY = sin * centerPrimeX + cos * centerPrimeY + (y + endY) / 2;
            double startAngle = Math.atan2((primeY - centerPrimeY) / radiusY,
                    (primeX - centerPrimeX) / radiusX);
            double delta = Math.atan2(
                    (-primeY - centerPrimeY) / radiusY,
                    (-primeX - centerPrimeX) / radiusX) - startAngle;
            if (!sweep && delta > 0) delta -= Math.PI * 2;
            if (sweep && delta < 0) delta += Math.PI * 2;

            int segments = Math.max(1, (int) Math.ceil(Math.abs(delta) / (Math.PI / 2)));
            double segment = delta / segments;
            for (int i = 0; i < segments; i++) {
                double a = startAngle + segment * i;
                double b = a + segment;
                double alpha = 4.0 / 3.0 * Math.tan((b - a) / 4.0);
                double p1x = Math.cos(a) - alpha * Math.sin(a);
                double p1y = Math.sin(a) + alpha * Math.cos(a);
                double p2x = Math.cos(b) + alpha * Math.sin(b);
                double p2y = Math.sin(b) - alpha * Math.cos(b);
                double[] c1 = ellipsePoint(p1x, p1y, centerX, centerY,
                        radiusX, radiusY, cos, sin);
                double[] c2 = ellipsePoint(p2x, p2y, centerX, centerY,
                        radiusX, radiusY, cos, sin);
                double[] end = ellipsePoint(Math.cos(b), Math.sin(b), centerX, centerY,
                        radiusX, radiusY, cos, sin);
                if (i == segments - 1) {
                    end[0] = endX;
                    end[1] = endY;
                }
                result.curveTo(c1[0], c1[1], c2[0], c2[1], end[0], end[1]);
            }
        }

        private double[] ellipsePoint(double px, double py, double cx, double cy,
                                      double rx, double ry, double cos, double sin) {
            return new double[] {
                    cx + rx * cos * px - ry * sin * py,
                    cy + rx * sin * px + ry * cos * py
            };
        }

        private double coordinate(boolean relative, double current) {
            double value = number();
            return relative ? current + value : value;
        }

        private double number() {
            skipSeparators();
            int start = index;
            if (index < path.length() && (path.charAt(index) == '+' || path.charAt(index) == '-')) {
                index++;
            }
            boolean digits = false;
            while (index < path.length() && Character.isDigit(path.charAt(index))) {
                index++;
                digits = true;
            }
            if (index < path.length() && path.charAt(index) == '.') {
                index++;
                while (index < path.length() && Character.isDigit(path.charAt(index))) {
                    index++;
                    digits = true;
                }
            }
            if (!digits) throw new IllegalArgumentException("Invalid SVG number in " + assetPath);
            if (index < path.length() && (path.charAt(index) == 'e' || path.charAt(index) == 'E')) {
                index++;
                if (index < path.length()
                        && (path.charAt(index) == '+' || path.charAt(index) == '-')) index++;
                int exponentStart = index;
                while (index < path.length() && Character.isDigit(path.charAt(index))) index++;
                if (index == exponentStart) {
                    throw new IllegalArgumentException("Invalid SVG exponent in " + assetPath);
                }
            }
            return Double.parseDouble(path.substring(start, index));
        }

        private void skipSeparators() {
            while (index < path.length()
                    && (Character.isWhitespace(path.charAt(index)) || path.charAt(index) == ',')) {
                index++;
            }
        }

        private static boolean isCubic(char command) {
            return Character.toUpperCase(command) == 'C'
                    || Character.toUpperCase(command) == 'S';
        }
    }
}
