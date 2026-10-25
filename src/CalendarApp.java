import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

public class CalendarApp extends JFrame {
    private YearMonth currentMonth = YearMonth.now();
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel grid = new JPanel(new GridLayout(0, 7));
    private final Map<LocalDate, Color> markedDays = new HashMap<>();
    private Color currentColor = new Color(120, 200, 120);
    private static final Path MARKS_FILE = Path.of(System.getProperty("user.home"), ".calendarapp", "marks.txt");

    public CalendarApp() {
        setTitle("Календарь");
        setSize(1280, 960);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");

        prevButton.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            render();
        });

        nextButton.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            render();
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(prevButton, BorderLayout.WEST);
        top.add(monthLabel, BorderLayout.CENTER);
        top.add(nextButton, BorderLayout.EAST);

        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.NORTH);
        north.add(createColorPanel(), BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);

        load();
        render();
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarApp());
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void render() {
        String name = currentMonth.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
        monthLabel.setText(capitalize(name) + " " + currentMonth.getYear());

        grid.removeAll();

        // Строка дней недели
        String[] names = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String n : names) {
            JLabel l = new JLabel(n, SwingConstants.CENTER);
            l.setFont(new Font("SansSerif", Font.BOLD, 12));
            l.setForeground(Color.GRAY);
            grid.add(l);
        }


        // Сетка чисел
        int shift = currentMonth.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < shift; i++) {
            grid.add(new JLabel(""));
        }

        LocalDate today = LocalDate.now();

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            JButton button = new JButton(String.valueOf(day));
            button.setFocusPainted(false);
            button.setFont(new Font("SansSerif", Font.PLAIN, 14));

            updateButtonStyle(button, date, today);

            button.addActionListener(e -> {
                Color existing = markedDays.get(date);
                if (existing == null) {
                    markedDays.put(date, currentColor);
                } else if (existing.equals(currentColor)) {
                    markedDays.remove(date);
                } else {
                    markedDays.put(date, currentColor);
                }
                updateButtonStyle(button, date, today);
                save();
            });

            grid.add(button);
        }

        int total = shift + currentMonth.lengthOfMonth();
        int rest = (7 - total % 7) % 7;
        for (int i = 0; i < rest; i++) {
            grid.add(new JLabel(""));
        }

        grid.revalidate();
        grid.repaint();

    }

    private void updateButtonStyle(JButton button, LocalDate date, LocalDate today) {
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        boolean marked = markedDays.containsKey(date);
        boolean isToday = date.equals(today);

        if (marked) {
            button.setBackground(markedDays.get(date));
        } else {
            button.setBackground(null);
        }

        if (isToday) {
            button.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0), 2));
        } else {
            button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
    }

    private JPanel createColorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        Color[] colors = {
                new Color(120, 200, 120),
                new Color(230, 100, 100),
                new Color(120, 160, 230)
        };

        for (Color c : colors) {
            JButton colorButton = new JButton("   ");
            colorButton.setBackground(c);
            colorButton.setOpaque(true);
            colorButton.setContentAreaFilled(true);
            colorButton.setFocusPainted(false);

            colorButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            colorButton.addActionListener(e -> {
                currentColor = c;
                for (Component comp : panel.getComponents()) {
                    if (comp instanceof JButton button) {
                        button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                    }
                }
                colorButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            });
            panel.add(colorButton);
        }
        return panel;
    }

    private void save() {
        try {
            List<String> lines = new ArrayList<>();

            for (Map.Entry<LocalDate, Color> entry : markedDays.entrySet()) {
                LocalDate date = entry.getKey();
                Color color = entry.getValue();

                String line = date.toString() + "=" + hexColor(color);
                lines.add(line);
            }

            Files.createDirectories(MARKS_FILE.getParent());
            Files.write(MARKS_FILE, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!Files.exists(MARKS_FILE)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(MARKS_FILE);
            for (String line : lines) {
                if (line.isBlank()) continue;

                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                LocalDate date = LocalDate.parse(parts[0]);
                Color color = Color.decode(parts[1]);

                markedDays.put(date, color);
            }

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    private String hexColor(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}