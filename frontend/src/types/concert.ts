export type SeatGrade = "VIP" | "R" | "S" | "A";

export type SeatDto = {
    id: number;
    seatNumber: string;
    grade: SeatGrade;
    price: number;
    locked: boolean;
    lockedBy?: number | null;
    reservedBy?: number | null;
};

export type ConcertDto = {
    id: number;
    groupId?: number | null;
    title: string;
    description?: string;
    venue: string;
    concertDate: string;
    startTime?: string;
    price?: number;
    totalTickets?: number;
    status: "OPEN" | "SOLD_OUT" | "CLOSED" | string;
    createdAt?: string;
    agencyId: number;
    ticketSaleDate?: string;
};

export type ConcertDetail = {
    id: number;
    title: string;
    description?: string;
    venue: string;
    concertDate: string;
    ticketSaleDate?: string;
    agencyId?: number;
};
