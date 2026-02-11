import React, {useState} from "react";
import {motion} from "framer-motion";
import Header from "./Header";
import { useEffect } from "react";
import { useLocation } from "react-router-dom";


const MainPage: React.FC = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const location = useLocation();

    useEffect(() => {
        if (location.state?.scrollToLogin) {
            const el = document.getElementById("login-section");
            el?.scrollIntoView({ behavior: "smooth" });
        }
    }, [location.state]);

    const fadeUp = {
        initial: {opacity: 0, y: -80},
        whileInView: {opacity: 1, y: 0},
        transition: {duration: 0.8},
        viewport: {once: true},
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await fetch("http://localhost:8080/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    username,
                    password,
                }),
            });

            if (!response.ok) {
                throw new Error("로그인 실패");
            }

            const data = await response.json();
            console.log("로그인 성공:", data);

            // 예시: 토큰 저장
            localStorage.setItem("accessToken", data.accessToken);

        } catch (error) {
            console.error(error);
            alert("아이디 또는 비밀번호를 확인해주세요.");
        }
    };

    return (
        <div className="h-screen overflow-hidden">
            <Header/>

            <main className="h-full scrollbar-hide overflow-y-scroll snap-y snap-mandatory  bg-idol-bg">

                {/* Slide 1 */}
                <section className="h-screen snap-start flex flex-col items-center content-center ">
                    <motion.div
                        {...fadeUp}
                        className="w-[80%] h-[50%] bg-white rounded-full content-center mt-[20%]  z-20">
                        <p className="transform text-3xl font-semibold leading-relaxed text-center mix-blend-multiply">
                            좋아하는 아이돌에게
                            <br/>
                            직접 <span className="text-idol">마음</span>을 전해보세요.
                        </p>

                    </motion.div>
                    <motion.div
                        initial={{opacity: 0, y: -80, skewY: 25}}
                        whileInView={{opacity: 1, y: 0, skewY: 25}}
                        transition={{duration: 0.5}}
                        viewport={{once: true}}
                        className="transform  w-full bg-idol-point p-4 mix-blend-normal z-30"
                    ></motion.div>
                    <motion.div
                        initial={{opacity: 0, y: -80, skewY: -15}}
                        whileInView={{opacity: 1, y: 0, skewY: -15}}
                        transition={{duration: 0.5}}
                        viewport={{once: true}}
                        className="transform mb-[10%]  w-full bg-idol-point p-4 mix-blend-normal z-10"
                    ></motion.div>
                </section>

                {/* Slide 2 */}
                <section className="h-screen snap-start content-center">
                    <div className="flex w-[80%] mx-[10%] h-1/2 items-center gap-16 bg-white rounded-full z-20">

                        {/* Image */}
                        <motion.div
                            initial={{opacity: 0, y: -80, rotate: -15}}
                            whileInView={{opacity: 1, y: 0, rotate: -15}}
                            transition={{duration: 0.8}}
                            viewport={{once: true}}
                            className="w-1/2 h-72 max-w-[350px]  bg-idol-point z-20"
                        />

                        {/* Text */}
                        <motion.div
                            {...fadeUp}
                            className="w-[80%] h-1/2 z-20"
                        >
                            <p className="text-xl font-semibold mb-4 z-20">채팅에 대한 설명</p>
                            <p className="text-sm text-gray-600 leading-relaxed z-20">
                                설명 조금 더
                                <br/>
                                하고싶은 설명이 있다면 여기에 더 쓸 수 있습니다.
                                <br/>
                                조금 더 긴 버전
                            </p>
                        </motion.div>
                    </div>
                    <motion.div
                        initial={{opacity: 0, y: -80, skewY: 10}}
                        whileInView={{opacity: 1, y: 0, skewY: 10}}
                        transition={{duration: 0.5}}
                        viewport={{once: true}}
                        className="transform  w-full bg-idol-point p-4 mix-blend-normal z-10"
                    ></motion.div>
                </section>

                {/* Slide 3 */}
                <section className="h-screen snap-center flex items-center px-20">
                    <div className="flex w-full h-1/2 p-3 items-center gap-16 bg-white rounded-full">

                        {/* Text */}
                        <motion.div
                            {...fadeUp}
                            className="w-[80%] h-1/2 p-6"
                        >
                            <p className="text-xl font-semibold mb-4">게시판에 대한 설명</p>
                            <p className="text-sm text-gray-600 leading-relaxed">
                                설명 조금 더
                                <br/>
                                필요하면 여기에 더 적을 수 있습니다.
                                <br/>
                                조금 더
                            </p>
                        </motion.div>

                        {/* Image */}
                        <motion.div
                            initial={{opacity: 0, y: -80, rotate: 15}}
                            whileInView={{opacity: 1, y: 0, rotate: 15}}
                            transition={{duration: 0.8}}
                            viewport={{once: true}}
                            className="w-1/2 h-72  max-w-[350px] bg-idol-point"
                        />
                    </div>
                </section>

                {/* Slide 4 */}
                <section id="login-section" className="h-screen snap-end flex flex-col items-center content-center">
                    <motion.div
                        initial={{opacity: 0, y: -80, skewY: 15}}
                        whileInView={{opacity: 1, y: 0, skewY: 15}}
                        transition={{duration: 0.5}}
                        viewport={{once: true}}
                        className="transform mt-[15%] w-full bg-idol-point p-4 mix-blend-normal z-30"
                    ></motion.div>
                    <div className="w-[80%] h-1/2  p-3 content-start bg-white rounded-full z-20">

                        <p className="text-2xl text-center font-semibold my-8">
                            지금 바로 시작해보세요.
                        </p>

                        <div className="content-center items-center">

                            <div className="w-full justify-center items-center">
                                <form onSubmit={handleLogin} className="flex flex-col items-center mt-6">
                                    <input type="text"
                                           id="user-id"
                                           name="user-id"
                                           placeholder="아이디를 입력하세요."
                                           onChange={(e) => setUsername(e.target.value)}
                                           className="mx-[15%] w-[250px] mb-2 rounded-lg appearance-none border border-gray-300 py-2 px-4 bg-white placeholder-gray-400 text-base
                                                focus:outline-none focus:ring-2 focus:ring-idol focus:border-transparent"/>

                                    <input type="password"
                                           id="user-pw"
                                           name="user-pw"
                                           placeholder="비밀번호를 입력하세요."
                                           onChange={(e) => setPassword(e.target.value)}
                                           className="mx-[15%] w-[250px] rounded-lg appearance-none border border-gray-300 py-2 px-4 bg-white placeholder-gray-400 text-base
                                                focus:outline-none focus:ring-2 focus:ring-idol focus:border-transparent"/>
                                    <button
                                        type="submit"
                                        className="flex-1/2 mx-[15%] w-[250px] my-2  px-6 py-2 rounded-md bg-idol text-white hover:cursor-pointer">로그인
                                    </button>
                                    <button
                                        type="button"
                                        className="flex-1/2 mx-[15%] w-[250px] mb-2  px-6 py-2 rounded-md bg-idol text-white hover:cursor-pointer">가입하기
                                    </button>
                                    <button
                                        type="button"
                                        className="flex-1/2 mx-[15%] w-[250px] mb-2  px-6 py-2 rounded-md bg-yellow-300 text-black hover:cursor-pointer">👁️‍🗨️카카오 계정으로 로그인
                                    </button>
                                </form>
                            </div>
                        </div>

                    </div>

                    <motion.div
                        initial={{opacity: 0, y: -80, skewY: -20}}
                        whileInView={{opacity: 1, y: 0, skewY: -20}}
                        transition={{duration: 0.5}}
                        viewport={{once: true}}
                        className="transform  w-full bg-idol-point p-4 mix-blend-normal z-10"
                    ></motion.div>
                </section>

            </main>
        </div>
    );
};

export default MainPage;
