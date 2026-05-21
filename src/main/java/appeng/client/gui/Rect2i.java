package appeng.client.gui;

import org.jspecify.annotations.NonNull;

public record Rect2i(int x, int y, int width, int height) {

    public boolean contains(int x, int y) {
        return x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rect2i(int x1, int y1, int width1, int height1))) {
            return false;
        }
        return x == x1 && y == y1 && width == width1 && height == height1;
    }

    @Override
    public @NonNull String toString() {
        return "Rect2i[x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }
}

