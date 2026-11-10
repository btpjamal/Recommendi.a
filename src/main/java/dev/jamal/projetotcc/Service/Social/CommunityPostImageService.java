package dev.jamal.projetotcc.Service.Social;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Service
public class CommunityPostImageService {
    public byte[] preparar(MultipartFile file) {
        if (file == null) return null;
        if (file.isEmpty() || file.getSize() > 1024 * 1024) throw invalida();
        try (var input=new MemoryCacheImageInputStream(file.getInputStream())) {
            var readers=ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalida();
            var reader=readers.next();
            try {
                if (!Set.of("jpeg","jpg","png").contains(reader.getFormatName().toLowerCase(Locale.ROOT))) throw invalida();
                reader.setInput(input,true,true);
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if(w<1 || h<1 || (long)w*h>16_000_000) throw invalida();
                var source=reader.read(0);
                double scale=Math.min(1,1200.0/Math.max(w,h));
                var image=new BufferedImage(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),BufferedImage.TYPE_INT_RGB);
                var g=image.createGraphics();
                try {
                    g.setColor(Color.WHITE);g.fillRect(0,0,image.getWidth(),image.getHeight());
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(source,0,0,image.getWidth(),image.getHeight(),null);
                } finally {g.dispose();}
                var bytes=new ByteArrayOutputStream();
                var writer=ImageIO.getImageWritersByFormatName("jpeg").next();
                try(var out=new MemoryCacheImageOutputStream(bytes)) {
                    writer.setOutput(out);
                    var options=writer.getDefaultWriteParam();
                    options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);options.setCompressionQuality(.85f);
                    writer.write(null,new IIOImage(image,null,null),options);
                } finally {writer.dispose();}
                return bytes.toByteArray();
            } finally {reader.dispose();}
        } catch(IOException | IllegalArgumentException e) {throw invalida();}
    }
    private ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use uma imagem PNG ou JPEG de até 1 MB e 16 megapixels.");
    }
}
