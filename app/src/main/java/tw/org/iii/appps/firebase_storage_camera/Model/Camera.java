package tw.org.iii.appps.firebase_storage_camera.Model;

public class Camera {
    private String camera;
    private String status = "0";

    public Camera() {
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
