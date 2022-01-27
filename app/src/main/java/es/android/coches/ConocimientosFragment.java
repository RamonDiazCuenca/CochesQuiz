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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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


    int contadorRespOK = 0;
    int contadorRespOKTotal = 0;
    JSONObject obj;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(todasLasPreguntas == null) {
            try {
                generarPreguntas("coches.xml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Collections.shuffle(todasLasPreguntas);
        preguntas = new ArrayList<>(todasLasPreguntas);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConocimientosBinding.inflate(inflater,container,false);

        presentarPregunta();

        binding.botonRespuesta.setOnClickListener(v -> {
            int seleccionado = binding.radioGroup.getCheckedRadioButtonId();
            //CharSequence mensaje = seleccionado == respuestaCorrecta ? "¡Acertaste!" : "Fallaste";

            CharSequence mensaje;
           if(seleccionado==respuestaCorrecta){

               mensaje = "¡Acertaste!";
               contadorRespOK++;

               if(contadorRespOKTotal<contadorRespOK)
                   contadorRespOKTotal=contadorRespOK;

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
                binding.bandera.setVisibility(View.GONE);
                binding.radioGroup.setVisibility(View.GONE);
                binding.botonRespuesta.setVisibility(View.GONE);
                binding.textView.setText("¡Fin! \n Acertaste: " + obj.getInt("ultima_puntuacion") + " \n Tu puntuación máxima es: " + obj.getInt("puntuacion_maxima"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void almacenarPuntuacion(int puntuacionActual, int puntuacionMaxima){


    }

    private void utilizarFichero(){

        String fichero = "JsonPuntuacion.json", textoAlmacenar = obj.toString();
        FileOutputStream fos;

        try {
            fos = openFileOutput(fichero, Context.MODE_PRIVATE);
            fos.write(textoAlmacenar.getBytes());
            fos.close();

        }catch (Exception e){
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