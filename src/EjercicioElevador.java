import simlib.*;

import java.io.*;
import java.util.PriorityQueue;
import java.util.Random;

public class EjercicioElevador {
    static final byte LLEGADA_A = 0, LLEGADA_B = 1, LLEGADA_C = 2, DESCARGA = 3, REGRESO = 4, FIN_SIM = 5;
    static final byte IDLE = 0, BUSSY = 1;
    static final int capacidad = 400;
    static int pesoATransportar;
    static Random random;
    static int maxA, minA, valB, distC;
    static double maxTime;
    static Timer simTime;
    static DiscreteStat transitoA, esperaB;
    static int totalC;
    static SimList< Box > cajasATransportar;
    static SimList< Event > eventos;
    static SimList< Box > cajasFaltantes;
    static ContinStat <Byte>elevador;
    static byte eventType;

    public static void main(String[]args)throws IOException {
        /* ABRIR ARCHIVOS */
        BufferedReader input = new BufferedReader( new FileReader("Input.txt") );
        BufferedWriter out = new BufferedWriter(new FileWriter("Output.txt"));

        /* LEER Y GUARDAR PARÁMETROS */
        String in = input.readLine();
        maxA = Integer.parseInt( in.split("-")[1] );
        minA = Integer.parseInt( in.split("-")[0] );
        valB = Integer.parseInt( input.readLine() );
        distC = Integer.parseInt( input.readLine() );
        maxTime = Double.parseDouble( input.readLine() );


        /* INICIALIZAR */
        inicializar();
        System.out.println("Init");
        do {
            sincronizar();
            switch ( eventType ) {
                case LLEGADA_A:
                    llegadaA();
                    break;
                case LLEGADA_B:
                    llegadaB();
                    break;
                case LLEGADA_C:
                    llegadaC();
                    break;
                case DESCARGA:
                    descarga();
                    break;
                case REGRESO:
                    regreso();
                    break;
                case FIN_SIM:
                    finSim( out );
                    break;
            }
        } while ( eventType != FIN_SIM );
        input.close();
        out.close();
    }

    /*********************
     *      RUTINAS      *
     *********************/

    /**
     * Rutina de sincronización: elimina de la cola de eventos el evento ya realizado,
     * actualiza el tiempo de la simulación y actualiza algunas variables.
     **/
    static void sincronizar() {
        eventType = eventos.getFirst().getType();
        simTime.setTime(eventos.getFirst().getTime());
        // Elimina el evento ya procesado
        eventos.removeFirst();

        //Actualiza acumuladores estadísticos
        cajasFaltantes.update(simTime.getTime());

        // Actualiza el tiempo de la simulación
    }

    /**
     * Rutina de inicialización: inicializa todas las colas y variables de la
     * simulación, programa en la lista de eventos la primera llegada de cada tipo
     * de caja y el fin de la simulación.
     */
    static void inicializar( ) {
        /* Para tener datos diferentes en cada simulación */
        random = new Random( );
        random.setSeed( System.nanoTime() );

        /* Inicializa el tiempo de la simulación en 0.0 */
        simTime = new Timer( );

        /* Inicializa la lista de eventos */
        eventos = new SimList<Event>("Lista de Eventos", 0, true);

        /* Programa la primera llevada de cada tipo de caja */
        eventos.add( new Event( LLEGADA_A , simTime.getTime()+distUniforme( maxA, minA ) ) );
        eventos.add( new Event( LLEGADA_B , simTime.getTime()+valB) );
        eventos.add( new Event( LLEGADA_C , simTime.getTime()+distC() ) );

        /* Programa el fin de la simulación */
        eventos.add( new Event( FIN_SIM , (float)maxTime ) );

        /* Inicializa las demás colas y listas */
        cajasATransportar = new SimList<Box>("Cajas a transportar", 0, false);
        cajasFaltantes = new SimList("Cajas faltantes", 0, false);

        /* Inicializa las variables de estado y acumuladores */
        elevador = new ContinStat((float)0.0, simTime.getTime(), "estado del elevador");
        transitoA = new DiscreteStat("Tiempo de tránsito para cajas A");
        esperaB = new DiscreteStat("Tiempo de espera para cajas B");
        totalC = 0;
        pesoATransportar = 0;
    }

    /**
     * RUTINAS DE EVENTOS
     **********************/

    /**
     * LLegada de una caja tipo A: Programa siguiente evento de este tipo. Si el
     * elevador está disponible verifica si puede ingresar la caja y subirlo, en
     * ese caso lo carga y programa su descarga, en caso contrario solo añade la
     * caja a la cola de cajas faltantes.
     */
    static void llegadaA() {
        /* Programa siguiente llegada de caja tipo A */
        eventos.add(new Event(LLEGADA_A, simTime.getTime() + distUniforme(maxA, minA)));
        
        if(elevador.getValue() == IDLE && pesoATransportar + 200 <= capacidad){
            cajasATransportar.add(new Box( simTime.getTime(), 'A'));
            pesoATransportar += 200;
            if (pesoATransportar == capacidad){
                cargarElevador();
                eventos.add( new Event(DESCARGA, simTime.getTime() + 3));
            }
        } else
            cajasFaltantes.addLast(new Box( simTime.getTime(), 'A'));
    }

    /**
     * LLegada de una caja tipo B: Programa siguiente evento de este tipo. Si el
     * elevador está disponible verifica si puede ingresar la caja y subirlo, en
     * ese caso lo carga y programa su descarga, en caso contrario solo añade la
     * caja a la cola de cajas faltantes.
     */
    static void llegadaB() {
        eventos.add(new Event(LLEGADA_B, simTime.getTime() + valB));
        
        if(elevador.getValue() == IDLE && pesoATransportar + 100 <= capacidad){
            cajasATransportar.add(new Box( simTime.getTime(), 'B'));
            pesoATransportar += 100;
            if (pesoATransportar == capacidad){
                cargarElevador();
                eventos.add( new Event(DESCARGA, simTime.getTime() + 3) );
            }
        } else
            cajasFaltantes.addLast(new Box( simTime.getTime(), 'B') );
    }

    /**
     * LLegada de una caja tipo B: Programa siguiente evento de este tipo. Si el
     * elevador está disponible verifica si puede ingresar la caja y subirlo, en
     * ese caso lo carga y programa su descarga, en caso contrario solo añade la
     * caja a la cola de cajas faltantes.
     */
    static void llegadaC() {
        eventos.add(new Event(LLEGADA_C, simTime.getTime() + distUniforme(maxA, minA)));
        
        if(elevador.getValue() == IDLE && pesoATransportar + 50 <= capacidad){
            cajasATransportar.add( new Box( simTime.getTime(), 'C') );
            pesoATransportar += 50;
            if (pesoATransportar == capacidad){
                cargarElevador();
                eventos.add( new Event(DESCARGA, simTime.getTime() + 3) );
            }
        } else
            cajasFaltantes.addLast( new Box( simTime.getTime(), 'C') );
    }

    /**
     * Descarga del elevador en 2do piso: programa regreso del elevador al 1er piso,
     * actualiza acumuladores estadísticos y variables de estado del sistema. Vacía
     * la cola de cajas a transportar.
     */
    static void descarga(){
        eventos.add(new Event(REGRESO, simTime.getTime() + 1));
        for (Box caja : cajasATransportar){
            if (caja.getBoxType() == 'A'){
                transitoA.recordDiscrete(simTime.getTime() - caja.getArriveTime());
            }
            pesoATransportar -= caja.getWeight();
        }
        cajasATransportar.clear();
    }

    /**
     * Regreso del elevador al 1er piso: marca el elevador disponible, mete las cajas
     * que quepan en el elevador en la lista de cajas a transportar, si la capacidad
     * se completa, carga el elevador y programa su descarga.
     */
    static void regreso(){
        elevador.recordContin( IDLE, simTime.getTime() );
        SimList<Box> cajasRestantes = new SimList<>();
        for(Box caja : cajasFaltantes){
            if (caja.getWeight() + pesoATransportar <= capacidad){
                cajasATransportar.add(caja);
                pesoATransportar += caja.getWeight();
            } else
                cajasRestantes.add(caja);
        }
        cajasFaltantes.clear();
        cajasFaltantes.addAll(cajasRestantes);
        if ( pesoATransportar == capacidad )
            eventos.add( new Event( DESCARGA, simTime.getTime() + 3 ) );
    }

    /**
     * Fin de la simulación: Actualiza una última vez las variables del sistema, y
     * guarda en el archivo los datos obtenidos para las medidas de desempeño.
     *
     * @param bw   archivo en el que se guardarán los datos.
     */
    static void finSim( BufferedWriter bw ) throws IOException {
        elevador.report(bw, simTime.getTime());
        transitoA.report(bw);
        esperaB.report(bw);
        bw.write("Promedio de cajas C transportadas por hora: "+totalC/simTime.getTime()*60);
        //System.exit(0);
    }

    /**
     * SUB RUTINA Cargar elevador: Marca el elevador ocupado y actualiza acumuladores
     * estadísticos.
     */
    static void cargarElevador(){
        elevador.recordContin(BUSSY, simTime.getTime());
        for ( Box caja : cajasATransportar ){
            switch ( caja.getBoxType() ){
                case 'B':
                    esperaB.recordDiscrete( simTime.getTime() - caja.getArriveTime() );
                    break;
                case 'C':
                    totalC ++;
                    break;
            }
        }
    }

    /**********************************
     *   DISTRIBUCIONES ESTADÍSTICAS  *
     **********************************/

    /**
     * Distribución aleatoria con distribución uniforme
     *
     * @param max   valor máximo que puede retornar la distribución
     * @param min   valor mínimo que puede retornar la distribución
     * @return  valor aleatorio uniformemente distribuido en el rango [min, max)
     */
    static float distUniforme( int max, int min ){
        return min + random.nextFloat()*( max-min );
    }

    /**
     * Distribución especial para la caja tipo C.
     *      – Opción 1: Distribución de la forma P(x)=X
     *      – Opción 2: Distribución de exponencial con media 6.
     *
     * @return variable aleatoria perteneciente a la distribución seleccionada.
     */
    static float distC(){
        double rand = random.nextDouble();
        if(distC == 0){
            if(rand<0.33){
                return 2;
            }  else {
                return 3;
            }
        } else {
            return distExponencial(6);
        }
    }

    static float disTriangular( double a, double b, double c ){
        double rand = random.nextDouble();
        double x;
        double aux;
        if( rand <= ((b-a)/c-a) ){
            aux = Math.sqrt(((c-a)*(b-a)*rand));
            x = a + aux;
        }else{
            aux = Math.sqrt((c-a)*(c-b)*(1-rand));
            x = c - aux;
        }
        return (float)x;
    }

    /**
     * Distribución exponencial
     *
     * @param lambda    1/media
     * @return  valor aleatorio con distribucuón exponencial.
     */
    static float distExponencial( double lambda ){
        return (float)(-1/lambda*Math.log(random.nextFloat()));
    }
}