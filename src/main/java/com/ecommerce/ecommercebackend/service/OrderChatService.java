package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.entity.PaymentMethod;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Answers authenticated, read-only questions about a customer's own orders.
 *
 * <p>Personal order data is resolved inside Spring Boot and is never sent to
 * the external LLM. Every lookup is scoped by the authenticated user's id.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderChatService {

    private static final Pattern ORDER_CODE_PATTERN =
            Pattern.compile("\\bORD-[A-Z0-9-]{6,}\\b", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_LISTED_ORDERS = 5;

    private final OrderRepository orderRepository;

    /**
     * Returns an answer when the message is an order-status intent; otherwise
     * returns empty so the normal product chatbot can handle the request.
     */
    @Transactional(readOnly = true)
    public Optional<String> answer(User user, String message) {
        if (!isOrderStatusQuestion(message)) {
            return Optional.empty();
        }

        if (user == null) {
            return Optional.of(
                    "Bạn cần [đăng nhập](/login) để mình kiểm tra trạng thái đơn hàng cá nhân.");
        }

        Matcher codeMatcher = ORDER_CODE_PATTERN.matcher(message);
        if (codeMatcher.find()) {
            String orderCode = codeMatcher.group().toUpperCase(Locale.ROOT);
            return Optional.of(orderRepository.findByOrderCodeAndUserId(orderCode, user.getId())
                    .map(this::formatOrderDetail)
                    .orElseGet(() -> "Mình không tìm thấy đơn " + orderCode
                            + " trong tài khoản đang đăng nhập. "
                            + "Bạn có thể kiểm tra tại [Đơn mua](/orders)."));
        }

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (orders.isEmpty()) {
            return Optional.of(
                    "Tài khoản của bạn chưa có đơn hàng nào. "
                            + "Sau khi đặt hàng, bạn có thể xem tại [Đơn mua](/orders).");
        }

        String normalized = normalize(message);
        Optional<OrderStatus> requestedStatus = detectRequestedStatus(normalized);
        if (requestedStatus.isPresent()) {
            List<Order> matches = orders.stream()
                    .filter(order -> order.getStatus() == requestedStatus.get())
                    .limit(MAX_LISTED_ORDERS)
                    .toList();
            if (matches.isEmpty()) {
                return Optional.of("Hiện bạn không có đơn nào ở trạng thái “"
                        + statusLabel(requestedStatus.get()) + "”. "
                        + "Bạn có thể xem tất cả tại [Đơn mua](/orders).");
            }
            return Optional.of(formatOrderList(
                    matches,
                    "Các đơn " + statusLabel(requestedStatus.get()).toLowerCase(Locale.ROOT)
                            + " của bạn"));
        }

        if (asksForOrderList(normalized)) {
            return Optional.of(formatOrderList(
                    orders.stream().limit(MAX_LISTED_ORDERS).toList(),
                    "Các đơn hàng gần đây của bạn"));
        }

        String answer = formatOrderDetail(orders.get(0));
        if (orders.size() > 1) {
            answer += "\n\nBạn còn " + (orders.size() - 1)
                    + " đơn khác. Hãy gửi mã `ORD-...` hoặc mở [Đơn mua](/orders) để xem.";
        }
        return Optional.of(answer);
    }

    boolean isOrderStatusQuestion(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (ORDER_CODE_PATTERN.matcher(message).find()) {
            return true;
        }

        String normalized = normalize(message);
        boolean mentionsOrder = normalized.contains("don hang")
                || normalized.contains("don mua")
                || normalized.contains("don cua toi")
                || normalized.contains("don nao")
                || normalized.contains("don gan nhat")
                || normalized.contains("don moi nhat");
        if (!mentionsOrder) {
            return false;
        }

        return normalized.contains("trang thai")
                || normalized.contains("dang o dau")
                || normalized.contains("toi dau")
                || normalized.contains("khi nao")
                || normalized.contains("theo doi")
                || normalized.contains("kiem tra")
                || normalized.contains("cua toi")
                || normalized.contains("gan nhat")
                || normalized.contains("moi nhat")
                || normalized.contains("dang giao")
                || normalized.contains("da giao")
                || normalized.contains("cho duyet")
                || normalized.contains("cho xac nhan")
                || normalized.contains("dang xu ly")
                || normalized.contains("hoan thanh")
                || normalized.contains("da huy")
                || normalized.contains("don nao")
                || normalized.contains("cac don")
                || normalized.contains("danh sach");
    }

    private Optional<OrderStatus> detectRequestedStatus(String normalized) {
        if (normalized.contains("cho duyet") || normalized.contains("cho xac nhan")) {
            return Optional.of(OrderStatus.PENDING);
        }
        if (normalized.contains("da thanh toan")) {
            return Optional.of(OrderStatus.PAID);
        }
        if (normalized.contains("dang xu ly") || normalized.contains("da duyet")) {
            return Optional.of(OrderStatus.PROCESSING);
        }
        if (normalized.contains("dang giao") || normalized.contains("van chuyen")) {
            return Optional.of(OrderStatus.SHIPPING);
        }
        if (normalized.contains("hoan thanh") || normalized.contains("da giao")) {
            return Optional.of(OrderStatus.COMPLETED);
        }
        if (normalized.contains("da huy") || normalized.contains("bi huy")) {
            return Optional.of(OrderStatus.CANCELLED);
        }
        return Optional.empty();
    }

    private boolean asksForOrderList(String normalized) {
        return normalized.contains("cac don")
                || normalized.contains("nhung don")
                || normalized.contains("danh sach")
                || normalized.contains("tat ca don")
                || normalized.contains("don nao");
    }

    private String formatOrderDetail(Order order) {
        return "Đơn " + order.getOrderCode() + " của bạn:\n"
                + "- Trạng thái: " + statusLabel(order.getStatus()) + "\n"
                + "- Tiến trình: " + progressDescription(order) + "\n"
                + "- Thanh toán: " + paymentLabel(order.getPaymentMethod()) + "\n"
                + "- Tổng tiền: " + formatVnd(order.getTotalAmount()) + "\n"
                + "- Cập nhật: " + formatDateTime(
                        order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt()) + "\n"
                + "- Dự kiến giao: " + formatDeliveryWindow(
                        order.getEstimatedDeliveryMinDate(),
                        order.getEstimatedDeliveryMaxDate()) + "\n\n"
                + "[Xem chi tiết đơn hàng](/orders/" + order.getId() + ")";
    }

    private String formatOrderList(List<Order> orders, String title) {
        StringBuilder answer = new StringBuilder(title).append(":\n");
        for (int index = 0; index < orders.size(); index++) {
            Order order = orders.get(index);
            answer.append(index + 1)
                    .append(". [")
                    .append(order.getOrderCode())
                    .append("](/orders/")
                    .append(order.getId())
                    .append(") — ")
                    .append(statusLabel(order.getStatus()))
                    .append(" — ")
                    .append(formatVnd(order.getTotalAmount()))
                    .append("\n");
        }
        answer.append("\nDữ liệu được lấy trực tiếp từ tài khoản đang đăng nhập.");
        return answer.toString();
    }

    private String progressDescription(Order order) {
        return switch (order.getStatus()) {
            case PENDING -> order.getPaymentMethod() == PaymentMethod.COD
                    ? "Cửa hàng đang chờ duyệt đơn."
                    : "Đơn đang chờ xác nhận thanh toán trước khi được duyệt.";
            case PAID -> "Thanh toán đã được xác nhận, đơn đang chờ cửa hàng duyệt.";
            case PROCESSING -> "Cửa hàng đã duyệt và đang chuẩn bị hàng.";
            case SHIPPING -> "Đơn đã được bàn giao cho đơn vị vận chuyển.";
            case COMPLETED -> "Đơn đã giao thành công.";
            case CANCELLED -> "Tiến trình đã kết thúc vì đơn đã bị hủy.";
        };
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Chờ duyệt";
            case PAID -> "Đã thanh toán";
            case PROCESSING -> "Đã duyệt · Đang xử lý";
            case SHIPPING -> "Đang giao";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String paymentLabel(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "Chưa cập nhật";
        }
        return switch (paymentMethod) {
            case COD -> "Thanh toán khi nhận hàng (COD)";
            case VNPAY -> "VNPAY";
            case MOMO -> "MoMo";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Chưa cập nhật" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatDeliveryWindow(LocalDate minDate, LocalDate maxDate) {
        if (minDate == null && maxDate == null) {
            return "Đang cập nhật";
        }
        if (minDate == null) {
            return maxDate.format(DATE_FORMATTER);
        }
        if (maxDate == null || minDate.equals(maxDate)) {
            return minDate.format(DATE_FORMATTER);
        }
        return minDate.format(DATE_FORMATTER) + " - " + maxDate.format(DATE_FORMATTER);
    }

    private String formatVnd(Long value) {
        if (value == null) {
            return "Chưa cập nhật";
        }
        return String.format(Locale.ROOT, "%,d ₫", value).replace(',', '.');
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
