package dev.jamal.projetotcc.Service.Social;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class CommunityPostImageTest {
    final CommunityPostImageService service=new CommunityPostImageService();
    @Test void imagemOpcionalNaoExigeUpload() {assertNull(service.preparar(null));}
    @Test void rejeitaArquivoVazioFalsoEOversize() {
        for(byte[] bytes:new byte[][]{new byte[0],"<svg></svg>".getBytes(),new byte[1024*1024+1]})
            assertThrows(ResponseStatusException.class,()->service.preparar(new MockMultipartFile("imagem","foto.png","image/png",bytes)));
    }
    @Test void reprocessaPngParaJpegLimitado() throws Exception {
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1600,800,BufferedImage.TYPE_INT_ARGB),"png",bytes);
        var result=service.preparar(new MockMultipartFile("imagem","foto.png","image/png",bytes.toByteArray()));
        var decoded=ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(1200,decoded.getWidth());assertEquals(600,decoded.getHeight());assertEquals((byte)0xff,result[0]);
        assertEquals(0xffffff,decoded.getRGB(0,0)&0xffffff);
    }
    @Test void dimensoesExcessivasSaoRecusadasAntesDeDecodificarPixels() throws Exception {
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4100,4000,BufferedImage.TYPE_BYTE_GRAY),"png",bytes);
        assertThrows(ResponseStatusException.class,()->service.preparar(new MockMultipartFile("imagem","foto.png","image/png",bytes.toByteArray())));
    }
}
