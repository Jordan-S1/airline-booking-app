package com.airlinebookingsystem.dto.booking;

import com.airlinebookingsystem.dto.passenger.PassengerRequest;

import java.util.List;

public record BookingRequest(
        Long flightId,
        String seatClass,
        List<PassengerRequest> passengers
) {}
