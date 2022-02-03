package es.android.coches;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import es.android.coches.databinding.FragmentConocimientosBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ConocimientosFragment extends Fragment {

    private FragmentConocimientosBinding binding;

    List<Pregunta> todasLasPreguntas;
    List<String> todasLasRespuestas;

    List<Pregunta> preguntas;
    int respuestaCorrecta;


    int contadorRespOK;
    int contadorRespOKTotal = 0;
    JSONObject obj = new JSONObject();
    String cadena;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contadorRespOK = 0;
        cadena="";

        if(todasLasPreguntas == null) {
            try {
                generarPreguntas("coches.xml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Collections.shuffle(todasLasPreguntas);
        preguntas = new ArrayList<>(todasLasPreguntas);


        if(!fileExists(getContext(), "JsonPuntuacion.json")){

            //  CREAMOS EL FICHERO
            try {

                obj.put("puntuacion_maxima",contadorRespOKTotal);
                obj.put("ultima_puntuacion",contadorRespOK);
                String jsonString = obj.toString();

                salvarFichero("JsonPuntuacion.json",jsonString);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    // GUARDAMOS EL FICHERO
    private void salvarFichero(String fichero, String texto) {

        FileOutputStream fos;

        try {

            fos = getContext().openFileOutput(fichero, Context.MODE_PRIVATE);
            fos.write(texto.getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // COMPROBAMOS SI EL FICHERO EXISTE
    public boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        if (file == null || !file.exists()) {
            return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConocimientosBinding.inflate(inflater,container,false);

        leerPuntuacion();
        presentarPregunta();

        binding.botonRespuesta.setOnClickListener(v -> {
            int seleccionado = binding.radioGroup.getCheckedRadioButtonId();
            //CharSequence mensaje = seleccionado == respuestaCorrecta ? "¡Acertaste!" : "Fallaste";

            CharSequence mensaje;
            if(seleccionado==respuestaCorrecta){

                mensaje = "¡Acertaste!";
                contadorRespOK++;

                if(contadorRespOKTotal<contadorRespOK) {
                    contadorRespOKTotal = contadorRespOK;
                    cadena = "¡Has batido tu récord de aciertos! Has alcanzado " + contadorRespOKTotal+" puntos";
                }

            }else{
                mensaje = "Fallaste";
            }

            Snackbar.make(v, mensaje, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Siguiente", v1 -> presentarPregunta())
                    .show();
            v.setEnabled(false);
        });

        return binding.getRoot();
    }

    private List<String> generarRespuestasPosibles(String respuestaCorrecta) {
        List<String> respuestasPosibles = new ArrayList<>();
        respuestasPosibles.add(respuestaCorrecta);

        List<String> respuestasIncorrectas = new ArrayList<>(todasLasRespuestas);
        respuestasIncorrectas.remove(respuestaCorrecta);

        for(int i=0; i<binding.radioGroup.getChildCount()-1; i++) {
            int indiceRespuesta = new Random().nextInt(respuestasIncorrectas.size());
            respuestasPosibles.add(respuestasIncorrectas.remove(indiceRespuesta));

        }
        Collections.shuffle(respuestasPosibles);
        return respuestasPosibles;
    }

    private void presentarPregunta() {

        if(preguntas.size() > 0) {
            binding.botonRespuesta.setEnabled(true);

            int pregunta = new Random().nextInt(preguntas.size());

            Pregunta preguntaActual = preguntas.remove(pregunta);
            preguntaActual.setRespuetas(generarRespuestasPosibles(preguntaActual.respuestaCorrecta));

            InputStream bandera = null;
            try {
                //bandera = getContext().getAssets().open(preguntaActual.foto);

                // IMPLEMENTED
                int idLogo = getResources().getIdentifier(preguntaActual.foto,"raw",getContext().getPackageName());
                bandera = getContext().getResources().openRawResource(idLogo);

                binding.bandera.setImageBitmap(BitmapFactory.decodeStream(bandera));
            } catch (Exception e) {
                e.printStackTrace();
            }

            binding.radioGroup.clearCheck();
            for (int i = 0; i < binding.radioGroup.getChildCount(); i++) {
                RadioButton radio = (RadioButton) binding.radioGroup.getChildAt(i);
                CharSequence respuesta = preguntaActual.getRespuetas().get(i);

                if (respuesta.equals(preguntaActual.respuestaCorrecta))
                    respuestaCorrecta = radio.getId();

                radio.setText(respuesta);
            }
        } else {

            try {

                obj.put("puntuacion_maxima",contadorRespOKTotal);
                obj.put("ultima_puntuacion", contadorRespOK);

                String jsonString = obj.toString();

                salvarFichero("JsonPuntuacion.json",jsonString);
                if(cadena.equals(""))
                    cadena = "Has conseguido "+contadorRespOK+ " puntos";

                binding.bandera.setVisibility(View.GONE);
                binding.radioGroup.setVisibility(View.GONE);
                binding.botonRespuesta.setVisibility(View.GONE);
                binding.textView.setText("¡Fin! \n" + cadena);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void leerPuntuacion(){
        try {
            if(fileExists(getContext(), "JsonPuntuacion.json"));{
                FileInputStream fis = getContext().openFileInput("JsonPuntuacion.json");
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);

                StringBuilder sb = new StringBuilder();
                String fileContent = br.readLine();
                while(fileContent!=null){
                    sb.append(fileContent);
                    fileContent=br.readLine();
                }
                fileContent = sb.toString();
                JSONObject objJson = new JSONObject(fileContent);
                contadorRespOKTotal = objJson.getInt("puntuacion_maxima");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    class Pregunta {
        private String nombre;
        private String foto;
        private String respuestaCorrecta;
        private List<String> respuetas;

        public Pregunta(String nombre, String foto) {
            this.nombre = nombre;
            this.foto = foto;
            this.respuestaCorrecta = nombre;
        }

        public List<String> getRespuetas() {
            return respuetas;
        }

        public void setRespuetas(List<String> respuetas) {
            this.respuetas = respuetas;
        }
    }

    private Document leerXML(String fichero) throws Exception {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder constructor = factory.newDocumentBuilder();
        //Document doc = constructor.parse(getContext().getAssets().open(fichero));

        //IMPLEMENTED
        int idRecurso = getResources().getIdentifier("coches", "raw", getContext().getPackageName());
        Document doc = constructor.parse(getContext().getResources().openRawResource(idRecurso));

        doc.getDocumentElement().normalize();
        return doc;
    }

    private void generarPreguntas(String fichero) throws Exception {
        todasLasPreguntas = new LinkedList<>();
        todasLasRespuestas = new LinkedList<>();
        Document doc = leerXML(fichero);
        Element documentElement = doc.getDocumentElement();
        NodeList paises = documentElement.getChildNodes();
        for(int i=0; i<paises.getLength(); i++) {
            if(paises.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element pais = (Element) paises.item(i);
                //String nombre = pais.getAttribute("nombre");
                String nombre = pais.getElementsByTagName("nombre").item(0).getTextContent();
                String foto = pais.getElementsByTagName("foto").item(0).getTextContent();
                todasLasPreguntas.add(new Pregunta(nombre, foto));
                todasLasRespuestas.add(nombre);
            }
        }
    }
}