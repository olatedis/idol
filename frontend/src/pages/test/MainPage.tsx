import React from "react";
import {motion} from "framer-motion";
import Header from "./Header";

const MainPage: React.FC = () => {
    return (
        <div className="h-screen overflow-hidden">
            <Header/>

            <main className="h-full overflow-y-scroll snap-y snap-mandatory  bg-idol-bg">

                {/* Slide 1 */}
                <section className="h-screen snap-start flex flex-col items-center content-center ">
                    <motion.div
                        initial={{opacity: 0, y: -80}}
                        whileInView={{opacity: 1, y: 0}}
                        transition={{duration: 0.8}}
                        viewport={{once: true}}
                        className="w-[80%] h-1/2 bg-white rounded-full content-center mt-[10%]  z-20">
                        <p className="transform text-3xl font-semibold leading-relaxed text-center mix-blend-multiply">
                            좋아하는 아이돌에게
                            <br/>
                            직접 <a className="text-idol">마음</a>을 전해보세요.
                        </p>

                    </motion.div>
                    <motion.div
                        initial={{opacity: 0, y: -80, skewY: 25}}
                        whileInView={{opacity: 1, y: 0, skewY: 25}}
                        transition={{duration: 0.5}}
                        viewport={{once: true}}
                        className="transform  w-full bg-idol-light p-4 mix-blend-normal z-30"
                    ></motion.div>
                    <motion.div
                        initial={{opacity: 0, y: -80, skewY: -15}}
                        whileInView={{opacity: 1, y: 0, skewY: -15}}
                        transition={{duration: 0.5}}
                        viewport={{once: true}}
                        className="transform  w-full bg-idol-light p-4 mix-blend-normal z-10"
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
                            className="w-1/2 h-72 max-w-[350px]  bg-idol-light z-20"
                        />

                        {/* Text */}
                        <motion.div
                            initial={{opacity: 0, x: 80}}
                            whileInView={{opacity: 1, x: 0}}
                            transition={{duration: 0.8, delay: 0.2}}
                            viewport={{once: true}}
                            className="w-1/2 z-20"
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
                        className="transform  w-full bg-idol-light p-4 mix-blend-normal z-10"
                    ></motion.div>
                </section>

                {/* Slide 3 */}
                <section className="h-screen snap-center flex items-center px-20">
                    <div className="flex w-full h-1/2 p-3 items-center gap-16 bg-white rounded-full">

                        {/* Text */}
                        <motion.div
                            initial={{opacity: 0, x: -80}}
                            whileInView={{opacity: 1, x: 0}}
                            transition={{duration: 0.8, delay: 0.2}}
                            viewport={{once: true}}
                            className="w-1/2"
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
                            className="w-1/2 h-72  max-w-[350px] bg-idol-light"
                        />
                    </div>
                </section>

                {/* Slide 4 */}
                <section className="h-screen snap-end flex flex-col items-center content-center">
                    <div className="w-2/3 h-1/2 mt-[10%] p-3 content-center bg-white rounded-full">

                        <p className="text-2xl text-center font-semibold mb-8">
                            지금 바로 시작해보세요.
                        </p>

                        <div className="flex gap-6 items-center w-full pr-[5%] pl-[5%]">
                            <button className="flex-1/2 px-6 py-2 rounded-md bg-idol text-white">login</button>
                            <button className="flex-1/2 px-6 py-2 rounded-md bg-idol text-white">register</button>
                        </div>
                    </div>
                </section>

            </main>
        </div>
    );
};

export default MainPage;
