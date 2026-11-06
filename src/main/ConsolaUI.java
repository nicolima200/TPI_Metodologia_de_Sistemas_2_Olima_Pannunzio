package main;

import java.util.Scanner;

public class ConsolaUI {

    private final Scanner scanner;

    public ConsolaUI() {
        this.scanner = new Scanner(System.in);
    }

    public void mostrar(String texto) {
        System.out.println(texto);
    }

    public void error(String texto) {
        System.err.println(texto);
    }

    public String leerTexto(String etiqueta) {
        System.out.print(etiqueta);
        return scanner.nextLine();
    }

    public int leerEntero(String etiqueta) {
        while (true) {
            System.out.print(etiqueta);
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Ingrese un numero valido: ");
            }
        }
    }

    public long leerId(String etiqueta) {
        while (true) {
            System.out.print(etiqueta);
            try {
                long valor = Long.parseLong(scanner.nextLine());
                if (valor > 0) {
                    return valor;
                }
                if (valor == 0) {
                    mostrar("Volviendo...");
                    pausa(2000);
                    return 0;
                }
                System.out.print("El id debe ser mayor a 0.\nIngrese el id (0 para cancelar): ");
            } catch (NumberFormatException e) {
                System.out.print("Ingrese un numero valido (0 para cancelar): ");
            }
        }
    }

    public void pausa(int milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
