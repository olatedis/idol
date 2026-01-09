import { useState } from "react";

type Concert = {
    id: string;
    title: string;
    startDate: string;
    endDate: string;
    place: string;
    poster: string;
};

export default function ApiTestPage() {
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [concerts, setConcerts] = useState<Concert[]>([]);
    const [loading, setLoading] = useState(false);

    const search = async () => {
        if (!startDate || !endDate) return;

        setLoading(true);

        const res = await fetch(
            `http://localhost:8080/api/concerts/idol?startDate=${startDate.replaceAll("-", "")}&endDate=${endDate.replaceAll("-", "")}&pageNum=1`
        );
        console.log(res);

        const data = await res.json();
        console.log(data)
        setConcerts(data);
        setLoading(false);
    };

    return (
        <div>
            <h1>아이돌 콘서트 조회</h1>

            <div>
                <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                />
                <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                />
                <button onClick={search}>검색</button>
            </div>

            {loading && <p>로딩 중...</p>}

            <ul>
                {concerts.map((c) => (
                    <li key={c.id}>
                        <strong>{c.title}</strong><br />
                        {c.startDate} ~ {c.endDate}<br />
                        {c.place}<br />
                        {c.poster && <img src={c.poster} width={100} />}
                    </li>
                ))}
            </ul>
        </div>
    );
}
