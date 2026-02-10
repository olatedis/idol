import React from "react";
import { motion } from "framer-motion";
import Header from "./Header";

const MainPage: React.FC = () => {
    return (
        <div className="h-screen overflow-hidden">
            <Header />

            <main className="h-full overflow-y-scroll snap-y snap-mandatory  bg-rose-300">

                {/* Slide 1 */}
                <section className="h-screen snap-start flex flex-col items-center pt-[35vh] text-center">
                    <p className="text-3xl font-semibold leading-relaxed">
                        좋아하는 아이돌에게
                        <br />
                        직접 <a className="text-white">마음</a>을 전해보세요.
                    </p>
                </section>

                {/* Slide 2 */}
                <section className="h-screen snap-start flex items-center px-20">
                    <div className="flex w-full items-center gap-16">

                        {/* Image */}
                        <motion.div
                            initial={{ opacity: 0, y: -80, rotate: -15 }}
                            whileInView={{ opacity: 1, y: 0, rotate: -15 }}
                            transition={{ duration: 0.8 }}
                            viewport={{ once: true }}
                            className="w-1/2 h-72 bg-gray-200"
                        />

                        {/* Text */}
                        <motion.div
                            initial={{ opacity: 0, x: 80 }}
                            whileInView={{ opacity: 1, x: 0 }}
                            transition={{ duration: 0.8, delay: 0.2 }}
                            viewport={{ once: true }}
                            className="w-1/2"
                        >
                            <p className="text-xl font-semibold mb-4">채팅에 대한 설명</p>
                            <p className="text-sm text-gray-600 leading-relaxed">
                                설명 조금 더
                                <br />
                                하고싶은 설명이 있다면 여기에 더 쓸 수 있습니다.
                                <br />
                                조금 더 긴 버전
                            </p>
                        </motion.div>
                    </div>
                </section>

                {/* Slide 3 */}
                <section className="h-screen snap-start flex items-center px-20">
                    <div className="flex w-full items-center gap-16">

                        {/* Text */}
                        <motion.div
                            initial={{ opacity: 0, x: -80 }}
                            whileInView={{ opacity: 1, x: 0 }}
                            transition={{ duration: 0.8, delay: 0.2 }}
                            viewport={{ once: true }}
                            className="w-1/2"
                        >
                            <p className="text-xl font-semibold mb-4">게시판에 대한 설명</p>
                            <p className="text-sm text-gray-600 leading-relaxed">
                                설명 조금 더
                                <br />
                                필요하면 여기에 더 적을 수 있습니다.
                                <br />
                                조금 더
                            </p>
                        </motion.div>

                        {/* Image */}
                        <motion.div
                            initial={{ opacity: 0, y: -80, rotate: 15 }}
                            whileInView={{ opacity: 1, y: 0, rotate: 15 }}
                            transition={{ duration: 0.8 }}
                            viewport={{ once: true }}
                            className="w-1/2 h-72 bg-gray-200"
                        />
                    </div>
                </section>

                {/* Slide 4 */}
                <section className="h-screen snap-start flex flex-col items-center pt-[35vh]">
                    <p className="text-2xl font-semibold mb-8">
                        지금 바로 시작해보세요.
                    </p>

                    <div className="flex gap-6">
                        <button className="px-6 py-2 border rounded-md border-idol bg-idol">login</button>
                        <button className="px-6 py-2 border rounded-md border-idol bg-idol">register</button>
                    </div>
                </section>

            </main>
        </div>
    );
};

export default MainPage;
